import { useState, useEffect, useRef, useCallback } from "react";

// ── 상수 ──────────────────────────────────────────────────────────────────────
const API_BASE = "http://localhost:8080/api";
const WS_URL = "http://localhost:8080/ws";

// ── 화면 타입 ─────────────────────────────────────────────────────────────────
const SCREEN = { HOME: "home", LOBBY: "lobby", GAME: "game", RESULT: "result" };

// ── 타이머 색상 ───────────────────────────────────────────────────────────────
const timerColor = (sec) => sec > 15 ? "#22c55e" : sec > 8 ? "#f59e0b" : "#ef4444";

// ── WebSocket mock (실제 환경에서는 SockJS+STOMP 사용) ─────────────────────────
// 데모용: 실제 백엔드 없이도 UI 작동
let wsCallbacks = {};
const mockBroadcast = (type, payload) => {
  setTimeout(() => wsCallbacks[type]?.forEach(fn => fn(payload)), 100);
};

// ── 메인 앱 ───────────────────────────────────────────────────────────────────
export default function App() {
  const [screen, setScreen] = useState(SCREEN.HOME);
  const [session, setSession] = useState(null); // { sessionId, roomCode, nickname, isHost }
  const [room, setRoom] = useState(null);
  const [gameState, setGameState] = useState(null);
  const [result, setResult] = useState(null);

  const handleRoomCreated = (s, r) => { setSession(s); setRoom(r); setScreen(SCREEN.LOBBY); };
  const handleGameStart = (gs) => { setGameState(gs); setScreen(SCREEN.GAME); };
  const handleGameOver = (res) => { setResult(res); setScreen(SCREEN.RESULT); };

  return (
    <div style={{ minHeight: "100vh", background: "var(--color-background-tertiary)", fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif" }}>
      {screen === SCREEN.HOME && <HomeScreen onRoomCreated={handleRoomCreated} />}
      {screen === SCREEN.LOBBY && <LobbyScreen session={session} room={room} setRoom={setRoom} onGameStart={handleGameStart} />}
      {screen === SCREEN.GAME && <GameScreen session={session} initialState={gameState} onGameOver={handleGameOver} />}
      {screen === SCREEN.RESULT && <ResultScreen result={result} onRestart={() => setScreen(SCREEN.HOME)} />}
    </div>
  );
}

// ── 홈 화면 ───────────────────────────────────────────────────────────────────
function HomeScreen({ onRoomCreated }) {
  const [nickname, setNickname] = useState("");
  const [roomCode, setRoomCode] = useState("");
  const [mode, setMode] = useState("create"); // create | join
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!nickname.trim()) { setError("닉네임을 입력해주세요."); return; }
    setLoading(true); setError("");
    try {
      // Demo 모드: 실제 API 대신 mock 데이터 사용
      const code = mode === "create" ? Math.random().toString(36).slice(2,8).toUpperCase() : roomCode.toUpperCase();
      const sessionId = crypto.randomUUID();
      const session = { sessionId, roomCode: code, nickname: nickname.trim(), isHost: mode === "create" };
      const room = {
        roomCode: code,
        status: "WAITING",
        players: [{ sessionId, nickname: nickname.trim(), isHost: mode === "create", isAlive: true }]
      };
      setTimeout(() => { onRoomCreated(session, room); setLoading(false); }, 500);
    } catch (e) {
      setError("오류가 발생했습니다. 다시 시도해주세요.");
      setLoading(false);
    }
  };

  return (
    <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", minHeight:"100vh", padding:"2rem" }}>
      {/* 타이틀 */}
      <div style={{ textAlign:"center", marginBottom:"3rem" }}>
        <div style={{ fontSize:"4rem", marginBottom:"0.5rem" }}>🔤</div>
        <h1 style={{ fontSize:"3rem", fontWeight:700, color:"var(--color-text-primary)", margin:0, letterSpacing:"-0.02em" }}>끝말잇기</h1>
        <p style={{ color:"var(--color-text-secondary)", fontSize:"1.1rem", marginTop:"0.5rem" }}>친구와 실시간으로 즐기는 단어 게임</p>
      </div>

      {/* 카드 */}
      <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-xl)", border:"0.5px solid var(--color-border-tertiary)", padding:"2rem", width:"100%", maxWidth:"400px" }}>
        {/* 탭 */}
        <div style={{ display:"flex", gap:"0.5rem", marginBottom:"1.5rem", background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-md)", padding:"4px" }}>
          {[["create","방 만들기"], ["join","방 참가"]].map(([m, label]) => (
            <button key={m} onClick={() => setMode(m)} style={{
              flex:1, padding:"0.6rem", borderRadius:"calc(var(--border-radius-md) - 2px)", border:"none",
              background: mode === m ? "var(--color-background-primary)" : "transparent",
              boxShadow: mode === m ? "0 1px 3px rgba(0,0,0,0.1)" : "none",
              color: mode === m ? "var(--color-text-primary)" : "var(--color-text-secondary)",
              fontWeight: mode === m ? 500 : 400, cursor:"pointer", fontSize:"0.95rem", transition:"all 0.15s"
            }}>{label}</button>
          ))}
        </div>

        <div style={{ display:"flex", flexDirection:"column", gap:"1rem" }}>
          <div>
            <label style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", display:"block", marginBottom:"6px" }}>닉네임</label>
            <input
              placeholder="게임에서 사용할 이름"
              value={nickname} onChange={e => setNickname(e.target.value)}
              onKeyDown={e => e.key === "Enter" && handleSubmit()}
              style={{ width:"100%", boxSizing:"border-box" }}
              maxLength={12}
            />
          </div>

          {mode === "join" && (
            <div>
              <label style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", display:"block", marginBottom:"6px" }}>방 코드</label>
              <input
                placeholder="6자리 코드 입력 (예: AB12CD)"
                value={roomCode} onChange={e => setRoomCode(e.target.value.toUpperCase())}
                onKeyDown={e => e.key === "Enter" && handleSubmit()}
                style={{ width:"100%", boxSizing:"border-box", textTransform:"uppercase", letterSpacing:"0.1em" }}
                maxLength={6}
              />
            </div>
          )}

          {error && <p style={{ color:"var(--color-text-danger)", fontSize:"0.875rem", margin:0 }}>{error}</p>}

          <button onClick={handleSubmit} disabled={loading} style={{
            background:"var(--color-text-primary)", color:"var(--color-background-primary)",
            border:"none", padding:"0.85rem", borderRadius:"var(--border-radius-md)",
            fontSize:"1rem", fontWeight:500, cursor:loading?"not-allowed":"pointer",
            opacity: loading ? 0.7 : 1, transition:"opacity 0.15s", marginTop:"0.5rem"
          }}>
            {loading ? "처리 중..." : mode === "create" ? "방 만들기" : "입장하기"}
          </button>
        </div>
      </div>

      <p style={{ color:"var(--color-text-tertiary)", fontSize:"0.8rem", marginTop:"1.5rem", textAlign:"center" }}>
        로그인 불필요 · 방 코드만으로 참가
      </p>
    </div>
  );
}

// ── 로비 화면 ──────────────────────────────────────────────────────────────────
function LobbyScreen({ session, room, setRoom, onGameStart }) {
  const [players, setPlayers] = useState(room?.players || []);
  const [copied, setCopied] = useState(false);

  // Demo: 시뮬레이션된 플레이어 입장
  useEffect(() => {
    if (!session?.isHost) return;
    const names = ["철수","영희","민준","서연","도윤"];
    let i = 0;
    const timer = setInterval(() => {
      if (i >= 2) { clearInterval(timer); return; }
      const fake = { sessionId: crypto.randomUUID(), nickname: names[i++], isAlive: true };
      setPlayers(prev => [...prev, fake]);
    }, 2500);
    return () => clearInterval(timer);
  }, []);

  const copyCode = () => {
    navigator.clipboard.writeText(session.roomCode).then(() => { setCopied(true); setTimeout(() => setCopied(false), 2000); });
  };

  const startGame = () => {
    onGameStart({
      currentTurnSessionId: session.sessionId,
      currentTurnNickname: session.nickname,
      lastWord: null,
      requiredChar: null,
      players,
      usedWords: [],
      timer: 30
    });
  };

  return (
    <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", minHeight:"100vh", padding:"2rem" }}>
      <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-xl)", border:"0.5px solid var(--color-border-tertiary)", padding:"2rem", width:"100%", maxWidth:"480px" }}>
        <h2 style={{ margin:"0 0 0.25rem", fontSize:"1.4rem", fontWeight:600 }}>게임 대기실</h2>
        <p style={{ color:"var(--color-text-secondary)", fontSize:"0.9rem", margin:"0 0 1.5rem" }}>2명 이상이 모이면 시작할 수 있습니다</p>

        {/* 방 코드 */}
        <div style={{ background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-md)", padding:"1rem 1.25rem", display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:"1.5rem" }}>
          <div>
            <p style={{ fontSize:"0.8rem", color:"var(--color-text-secondary)", margin:"0 0 4px" }}>방 코드</p>
            <p style={{ fontSize:"1.8rem", fontWeight:700, letterSpacing:"0.2em", margin:0, fontFamily:"monospace" }}>{session.roomCode}</p>
          </div>
          <button onClick={copyCode} style={{ background:"none", border:"0.5px solid var(--color-border-secondary)", borderRadius:"var(--border-radius-md)", padding:"0.5rem 1rem", cursor:"pointer", color:"var(--color-text-secondary)", fontSize:"0.875rem" }}>
            {copied ? "✓ 복사됨" : "복사"}
          </button>
        </div>

        {/* 플레이어 목록 */}
        <div style={{ marginBottom:"1.5rem" }}>
          <p style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", margin:"0 0 0.75rem" }}>참가자 ({players.length}/8)</p>
          <div style={{ display:"flex", flexDirection:"column", gap:"0.5rem" }}>
            {players.map(p => (
              <div key={p.sessionId} style={{ display:"flex", alignItems:"center", gap:"0.75rem", padding:"0.65rem 0.875rem", background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-md)" }}>
                <div style={{ width:32, height:32, borderRadius:"50%", background:"var(--color-background-info)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:"0.8rem", fontWeight:600, color:"var(--color-text-info)", flexShrink:0 }}>
                  {p.nickname[0]}
                </div>
                <span style={{ fontWeight: p.sessionId === session.sessionId ? 500 : 400 }}>{p.nickname}</span>
                {p.isHost && <span style={{ marginLeft:"auto", fontSize:"0.75rem", background:"var(--color-background-warning)", color:"var(--color-text-warning)", padding:"2px 8px", borderRadius:"999px" }}>방장</span>}
                {p.sessionId === session.sessionId && !p.isHost && <span style={{ marginLeft:"auto", fontSize:"0.75rem", color:"var(--color-text-tertiary)" }}>나</span>}
              </div>
            ))}
          </div>
        </div>

        {session.isHost ? (
          <button onClick={startGame} disabled={players.length < 2} style={{
            width:"100%", background: players.length >= 2 ? "var(--color-text-primary)" : "var(--color-background-secondary)",
            color: players.length >= 2 ? "var(--color-background-primary)" : "var(--color-text-tertiary)",
            border:"none", padding:"0.875rem", borderRadius:"var(--border-radius-md)", fontSize:"1rem",
            fontWeight:500, cursor: players.length >= 2 ? "pointer" : "not-allowed"
          }}>
            {players.length < 2 ? "참가자를 기다리는 중..." : "게임 시작"}
          </button>
        ) : (
          <div style={{ textAlign:"center", color:"var(--color-text-secondary)", fontSize:"0.9rem", padding:"0.875rem" }}>
            방장이 게임을 시작할 때까지 기다려주세요
          </div>
        )}
      </div>
    </div>
  );
}

// ── 게임 화면 ──────────────────────────────────────────────────────────────────
function GameScreen({ session, initialState, onGameOver }) {
  const [currentTurn, setCurrentTurn] = useState(initialState?.currentTurnSessionId);
  const [currentNickname, setCurrentNickname] = useState(initialState?.currentTurnNickname);
  const [lastWord, setLastWord] = useState(null);
  const [requiredChar, setRequiredChar] = useState(null);
  const [wordInput, setWordInput] = useState("");
  const [wordHistory, setWordHistory] = useState([]);
  const [players, setPlayers] = useState(initialState?.players || []);
  const [timer, setTimer] = useState(30);
  const [chatInput, setChatInput] = useState("");
  const [chatMessages, setChatMessages] = useState([]);
  const [error, setError] = useState("");
  const [pulse, setPulse] = useState(false);
  const chatEndRef = useRef(null);
  const isMyTurn = currentTurn === session?.sessionId;

  // 타이머
  useEffect(() => {
    const interval = setInterval(() => {
      setTimer(prev => {
        if (prev <= 1) {
          // 타임아웃: 데모에서는 다음 턴으로
          handleTimeout();
          return 30;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [currentTurn]);

  const handleTimeout = useCallback(() => {
    const alive = players.filter(p => p.isAlive);
    const idx = alive.findIndex(p => p.sessionId === currentTurn);
    const next = alive[(idx + 1) % alive.length];
    if (next) { setCurrentTurn(next.sessionId); setCurrentNickname(next.nickname); }
    setTimer(30);
  }, [players, currentTurn]);

  const submitWord = () => {
    if (!isMyTurn || !wordInput.trim()) return;
    const word = wordInput.trim();

    // 끝글자 검증
    if (requiredChar && word[0] !== requiredChar) {
      setError(`'${requiredChar}'(으)로 시작하는 단어를 입력하세요.`);
      return;
    }

    setError("");
    setLastWord(word);
    setRequiredChar(word[word.length - 1]);
    setWordHistory(prev => [...prev, { word, nickname: session.nickname, sessionId: session.sessionId }]);
    setWordInput("");
    setPulse(true);
    setTimeout(() => setPulse(false), 400);

    // 다음 턴으로
    const alive = players.filter(p => p.isAlive);
    const idx = alive.findIndex(p => p.sessionId === currentTurn);
    const next = alive[(idx + 1) % alive.length];
    if (next) { setCurrentTurn(next.sessionId); setCurrentNickname(next.nickname); }
    setTimer(30);

    // 데모: 5단어 후 게임 종료
    if (wordHistory.length >= 4) {
      setTimeout(() => onGameOver({ winner: session.nickname, words: [...wordHistory, { word, nickname: session.nickname }], players }), 800);
    }
  };

  const sendChat = () => {
    if (!chatInput.trim()) return;
    setChatMessages(prev => [...prev, { nickname: session.nickname, content: chatInput.trim(), time: new Date().toLocaleTimeString("ko-KR", { hour:"2-digit", minute:"2-digit" }) }]);
    setChatInput("");
    setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior:"smooth" }), 50);
  };

  const timerPct = (timer / 30) * 100;

  return (
    <div style={{ display:"grid", gridTemplateColumns:"1fr 320px", gap:"1rem", padding:"1rem", maxWidth:1100, margin:"0 auto", minHeight:"100vh", alignContent:"start" }}>

      {/* 왼쪽: 게임 영역 */}
      <div style={{ display:"flex", flexDirection:"column", gap:"1rem" }}>

        {/* 턴 정보 + 타이머 */}
        <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border:"0.5px solid var(--color-border-tertiary)", padding:"1.25rem" }}>
          <div style={{ display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:"0.75rem" }}>
            <div>
              <p style={{ fontSize:"0.8rem", color:"var(--color-text-secondary)", margin:"0 0 4px" }}>현재 차례</p>
              <p style={{ fontSize:"1.3rem", fontWeight:600, margin:0 }}>
                {isMyTurn ? "🎯 나의 차례!" : `${currentNickname}의 차례`}
              </p>
            </div>
            <div style={{ textAlign:"right" }}>
              <p style={{ fontSize:"0.8rem", color:"var(--color-text-secondary)", margin:"0 0 4px" }}>남은 시간</p>
              <p style={{ fontSize:"2rem", fontWeight:700, margin:0, color: timerColor(timer), transition:"color 0.3s", fontFamily:"monospace" }}>{timer}s</p>
            </div>
          </div>
          {/* 타이머 바 */}
          <div style={{ height:6, background:"var(--color-background-secondary)", borderRadius:999, overflow:"hidden" }}>
            <div style={{ height:"100%", width:`${timerPct}%`, background: timerColor(timer), borderRadius:999, transition:"width 1s linear, background 0.3s" }} />
          </div>
        </div>

        {/* 제출된 단어 */}
        {lastWord && (
          <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border:"0.5px solid var(--color-border-tertiary)", padding:"1.25rem", textAlign:"center" }}>
            <p style={{ fontSize:"0.8rem", color:"var(--color-text-secondary)", margin:"0 0 8px" }}>이전 단어</p>
            <p style={{ fontSize:"2.5rem", fontWeight:700, margin:"0 0 8px", transition:"transform 0.3s", transform: pulse ? "scale(1.05)" : "scale(1)" }}>{lastWord}</p>
            <p style={{ fontSize:"1rem", color:"var(--color-text-secondary)", margin:0 }}>다음은 <strong style={{ color:"var(--color-text-primary)", fontSize:"1.3rem" }}>'{requiredChar}'</strong>으로 시작하는 단어</p>
          </div>
        )}

        {!lastWord && (
          <div style={{ background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-lg)", padding:"1.5rem", textAlign:"center" }}>
            <p style={{ fontSize:"1rem", color:"var(--color-text-secondary)", margin:0 }}>첫 번째 단어를 입력하세요 — 어떤 단어든 가능합니다!</p>
          </div>
        )}

        {/* 단어 입력 */}
        <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border: isMyTurn ? "2px solid var(--color-border-info)" : "0.5px solid var(--color-border-tertiary)", padding:"1.25rem" }}>
          <div style={{ display:"flex", gap:"0.75rem" }}>
            <input
              placeholder={isMyTurn ? (requiredChar ? `'${requiredChar}'(으)로 시작하는 단어` : "첫 단어를 입력하세요") : `${currentNickname}의 차례입니다`}
              value={wordInput}
              onChange={e => { setWordInput(e.target.value); setError(""); }}
              onKeyDown={e => e.key === "Enter" && submitWord()}
              disabled={!isMyTurn}
              style={{ flex:1 }}
            />
            <button onClick={submitWord} disabled={!isMyTurn || !wordInput.trim()} style={{
              background: isMyTurn && wordInput.trim() ? "var(--color-text-primary)" : "var(--color-background-secondary)",
              color: isMyTurn && wordInput.trim() ? "var(--color-background-primary)" : "var(--color-text-tertiary)",
              border:"none", padding:"0 1.25rem", borderRadius:"var(--border-radius-md)", cursor: isMyTurn && wordInput.trim() ? "pointer" : "not-allowed",
              fontWeight:500, whiteSpace:"nowrap", transition:"all 0.15s"
            }}>제출</button>
          </div>
          {error && <p style={{ color:"var(--color-text-danger)", fontSize:"0.875rem", margin:"0.5rem 0 0" }}>{error}</p>}
        </div>

        {/* 단어 히스토리 */}
        <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border:"0.5px solid var(--color-border-tertiary)", padding:"1.25rem" }}>
          <p style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", margin:"0 0 0.75rem" }}>사용된 단어 ({wordHistory.length}개)</p>
          <div style={{ display:"flex", flexWrap:"wrap", gap:"0.5rem", maxHeight:160, overflowY:"auto" }}>
            {wordHistory.length === 0 && <p style={{ color:"var(--color-text-tertiary)", fontSize:"0.875rem", margin:0 }}>아직 없음</p>}
            {[...wordHistory].reverse().map((w, i) => (
              <span key={i} style={{ fontSize:"0.875rem", padding:"4px 10px", background:"var(--color-background-secondary)", borderRadius:"999px", color:"var(--color-text-secondary)" }}>
                {w.word}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* 오른쪽: 사이드바 */}
      <div style={{ display:"flex", flexDirection:"column", gap:"1rem" }}>
        {/* 플레이어 목록 */}
        <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border:"0.5px solid var(--color-border-tertiary)", padding:"1.25rem" }}>
          <p style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", margin:"0 0 0.75rem" }}>플레이어</p>
          <div style={{ display:"flex", flexDirection:"column", gap:"0.5rem" }}>
            {players.map(p => (
              <div key={p.sessionId} style={{ display:"flex", alignItems:"center", gap:"0.6rem", padding:"0.5rem 0.75rem", borderRadius:"var(--border-radius-md)", background: p.sessionId === currentTurn ? "var(--color-background-info)" : "transparent", transition:"background 0.2s" }}>
                <div style={{ width:28, height:28, borderRadius:"50%", background: p.isAlive ? "var(--color-background-success)" : "var(--color-background-secondary)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:"0.75rem", fontWeight:600, color: p.isAlive ? "var(--color-text-success)" : "var(--color-text-tertiary)", flexShrink:0 }}>
                  {p.nickname[0]}
                </div>
                <span style={{ fontSize:"0.9rem", color: p.isAlive ? "var(--color-text-primary)" : "var(--color-text-tertiary)", textDecoration: p.isAlive ? "none" : "line-through" }}>{p.nickname}</span>
                {p.sessionId === currentTurn && <span style={{ marginLeft:"auto", fontSize:"0.7rem", background:"var(--color-background-info)", color:"var(--color-text-info)", padding:"2px 6px", borderRadius:"999px" }}>턴</span>}
              </div>
            ))}
          </div>
        </div>

        {/* 채팅 */}
        <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border:"0.5px solid var(--color-border-tertiary)", padding:"1.25rem", display:"flex", flexDirection:"column", flex:1, minHeight:280 }}>
          <p style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", margin:"0 0 0.75rem" }}>채팅</p>
          <div style={{ flex:1, overflowY:"auto", display:"flex", flexDirection:"column", gap:"0.4rem", marginBottom:"0.75rem", minHeight:200, maxHeight:240 }}>
            {chatMessages.length === 0 && <p style={{ color:"var(--color-text-tertiary)", fontSize:"0.8rem", textAlign:"center", marginTop:"2rem" }}>채팅을 시작해보세요</p>}
            {chatMessages.map((m, i) => (
              <div key={i} style={{ fontSize:"0.85rem" }}>
                <span style={{ fontWeight:500, color: m.nickname === session?.nickname ? "var(--color-text-info)" : "var(--color-text-primary)" }}>{m.nickname}</span>
                <span style={{ color:"var(--color-text-tertiary)", fontSize:"0.75rem", marginLeft:"6px" }}>{m.time}</span>
                <p style={{ margin:"2px 0 0", color:"var(--color-text-secondary)", wordBreak:"break-all" }}>{m.content}</p>
              </div>
            ))}
            <div ref={chatEndRef} />
          </div>
          <div style={{ display:"flex", gap:"0.5rem" }}>
            <input placeholder="메시지 입력" value={chatInput} onChange={e => setChatInput(e.target.value)} onKeyDown={e => e.key === "Enter" && sendChat()} style={{ flex:1, fontSize:"0.875rem" }} />
            <button onClick={sendChat} disabled={!chatInput.trim()} style={{ border:"0.5px solid var(--color-border-secondary)", background:"none", padding:"0 0.75rem", borderRadius:"var(--border-radius-md)", cursor:"pointer", color:"var(--color-text-secondary)", fontSize:"0.85rem" }}>전송</button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── 결과 화면 ──────────────────────────────────────────────────────────────────
function ResultScreen({ result, onRestart }) {
  return (
    <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", minHeight:"100vh", padding:"2rem" }}>
      <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-xl)", border:"0.5px solid var(--color-border-tertiary)", padding:"2.5rem", width:"100%", maxWidth:"480px", textAlign:"center" }}>
        <div style={{ fontSize:"4rem", marginBottom:"1rem" }}>🏆</div>
        <h2 style={{ fontSize:"1.8rem", fontWeight:700, margin:"0 0 0.5rem" }}>게임 종료!</h2>
        <p style={{ color:"var(--color-text-secondary)", marginBottom:"2rem" }}>
          <strong style={{ color:"var(--color-text-primary)", fontSize:"1.2rem" }}>{result?.winner}</strong>님이 우승했습니다!
        </p>

        <div style={{ background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-lg)", padding:"1.25rem", marginBottom:"1.5rem", textAlign:"left" }}>
          <p style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", margin:"0 0 0.75rem" }}>사용된 단어 ({result?.words?.length || 0}개)</p>
          <div style={{ display:"flex", flexWrap:"wrap", gap:"0.4rem" }}>
            {result?.words?.map((w, i) => (
              <span key={i} style={{ fontSize:"0.875rem", padding:"3px 10px", background:"var(--color-background-primary)", border:"0.5px solid var(--color-border-tertiary)", borderRadius:"999px" }}>
                {w.word}
              </span>
            ))}
          </div>
        </div>

        <button onClick={onRestart} style={{ background:"var(--color-text-primary)", color:"var(--color-background-primary)", border:"none", padding:"0.875rem 2rem", borderRadius:"var(--border-radius-md)", fontSize:"1rem", fontWeight:500, cursor:"pointer", width:"100%" }}>
          처음으로 돌아가기
        </button>
      </div>
    </div>
  );
}
