import { useState, useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

// ── 상수 ──────────────────────────────────────────────────────────────────────
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";
const WS_URL = import.meta.env.VITE_WS_URL ?? "http://localhost:8080/ws";
const SCREEN = { HOME: "home", LOBBY: "lobby", GAME: "game", RESULT: "result" };
const timerColor = (sec) => sec > 15 ? "#22c55e" : sec > 8 ? "#f59e0b" : "#ef4444";

// ── 메인 앱 ───────────────────────────────────────────────────────────────────
export default function App() {
  const [screen, setScreen] = useState(SCREEN.HOME);
  const [session, setSession] = useState(null); // { sessionId, roomCode, nickname, isHost, players }
  const [gameInit, setGameInit] = useState(null);
  const [result, setResult] = useState(null);
  const [wsReady, setWsReady] = useState(false);
  const stompRef = useRef(null);

  const connectWs = (sessionId) => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { "session-id": sessionId },
      reconnectDelay: 3000,
      onConnect: () => setWsReady(true),
      onDisconnect: () => setWsReady(false),
    });
    stompRef.current = client;
    client.activate();
  };

  const disconnectWs = () => {
    stompRef.current?.deactivate();
    stompRef.current = null;
    setWsReady(false);
  };

  const handleRoomReady = (sess) => {
    setSession(sess);
    connectWs(sess.sessionId);
    setScreen(SCREEN.LOBBY);
  };

  const handleGameStart = (payload) => {
    setGameInit(payload);
    setScreen(SCREEN.GAME);
  };

  const handleGameOver = (payload) => {
    setResult(payload);
    setScreen(SCREEN.RESULT);
  };

  const handleRestart = () => {
    disconnectWs();
    setSession(null);
    setGameInit(null);
    setResult(null);
    setScreen(SCREEN.HOME);
  };

  return (
    <div style={{ minHeight: "100vh", background: "var(--color-background-tertiary)", fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif" }}>
      {screen === SCREEN.HOME && <HomeScreen onRoomReady={handleRoomReady} />}
      {screen === SCREEN.LOBBY && (
        <LobbyScreen
          session={session}
          stompRef={stompRef}
          wsReady={wsReady}
          onGameStart={handleGameStart}
          onRoomClosed={handleRestart}
        />
      )}
      {screen === SCREEN.GAME && (
        <GameScreen
          session={session}
          stompRef={stompRef}
          wsReady={wsReady}
          initState={gameInit}
          onGameOver={handleGameOver}
        />
      )}
      {screen === SCREEN.RESULT && <ResultScreen result={result} onRestart={handleRestart} />}
    </div>
  );
}

// ── 홈 화면 ───────────────────────────────────────────────────────────────────
function HomeScreen({ onRoomReady }) {
  const [nickname, setNickname] = useState("");
  const [roomCode, setRoomCode] = useState("");
  const [mode, setMode] = useState("create");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!nickname.trim()) { setError("닉네임을 입력해주세요."); return; }
    if (mode === "join" && !roomCode.trim()) { setError("방 코드를 입력해주세요."); return; }
    setLoading(true); setError("");
    try {
      let data;
      if (mode === "create") {
        const res = await fetch(`${API_BASE}/rooms`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ nickname: nickname.trim() }),
        });
        const json = await res.json();
        if (!json.success) throw new Error(json.error || "방 생성에 실패했습니다.");
        data = json.data;
      } else {
        const code = roomCode.trim().toUpperCase();
        const res = await fetch(`${API_BASE}/rooms/${code}/join`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ nickname: nickname.trim(), roomCode: code }),
        });
        const json = await res.json();
        if (!json.success) throw new Error(json.error || "방 참가에 실패했습니다.");
        data = json.data;
      }
      onRoomReady({
        sessionId: data.sessionId,
        roomCode: data.roomCode,
        nickname: nickname.trim(),
        isHost: mode === "create",
        players: data.players,
      });
    } catch (e) {
      setError(e.message || "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", minHeight:"100vh", padding:"2rem" }}>
      <div style={{ textAlign:"center", marginBottom:"3rem" }}>
        <div style={{ fontSize:"4rem", marginBottom:"0.5rem" }}>🔤</div>
        <h1 style={{ fontSize:"3rem", fontWeight:700, color:"var(--color-text-primary)", margin:0, letterSpacing:"-0.02em" }}>끝말잇기</h1>
        <p style={{ color:"var(--color-text-secondary)", fontSize:"1.1rem", marginTop:"0.5rem" }}>친구와 실시간으로 즐기는 단어 게임</p>
      </div>

      <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-xl)", border:"0.5px solid var(--color-border-tertiary)", padding:"2rem", width:"100%", maxWidth:"400px" }}>
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
function LobbyScreen({ session, stompRef, wsReady, onGameStart, onRoomClosed }) {
  const [players, setPlayers] = useState(session?.players || []);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!wsReady || !stompRef.current) return;

    const sub = stompRef.current.subscribe(`/topic/room/${session.roomCode}/game`, (msg) => {
      const { type, payload } = JSON.parse(msg.body);
      switch (type) {
        case "PLAYER_JOINED":
        case "PLAYER_LEFT":
          setPlayers(payload.players);
          break;
        case "GAME_START":
          onGameStart(payload);
          break;
        case "HOST_LEFT":
          alert(payload?.message || "방장이 퇴장하여 방이 종료됩니다.");
          onRoomClosed();
          break;
        case "ROOM_CLOSED":
          onRoomClosed();
          break;
      }
    });

    return () => sub.unsubscribe();
  }, [wsReady]);

  const copyCode = () => {
    navigator.clipboard.writeText(session.roomCode)
      .then(() => { setCopied(true); setTimeout(() => setCopied(false), 2000); });
  };

  const startGame = async () => {
    setError("");
    try {
      const res = await fetch(`${API_BASE}/rooms/${session.roomCode}/start`, {
        method: "POST",
        headers: { "X-Session-Id": session.sessionId },
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error || "게임 시작에 실패했습니다.");
      // 게임 시작 성공 → GAME_START 이벤트가 WebSocket으로 수신됨
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", minHeight:"100vh", padding:"2rem" }}>
      <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-xl)", border:"0.5px solid var(--color-border-tertiary)", padding:"2rem", width:"100%", maxWidth:"480px" }}>
        <h2 style={{ margin:"0 0 0.25rem", fontSize:"1.4rem", fontWeight:600 }}>게임 대기실</h2>
        <p style={{ color:"var(--color-text-secondary)", fontSize:"0.9rem", margin:"0 0 1.5rem" }}>2명 이상이 모이면 시작할 수 있습니다</p>

        <div style={{ background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-md)", padding:"1rem 1.25rem", display:"flex", alignItems:"center", justifyContent:"space-between", marginBottom:"1.5rem" }}>
          <div>
            <p style={{ fontSize:"0.8rem", color:"var(--color-text-secondary)", margin:"0 0 4px" }}>방 코드</p>
            <p style={{ fontSize:"1.8rem", fontWeight:700, letterSpacing:"0.2em", margin:0, fontFamily:"monospace" }}>{session.roomCode}</p>
          </div>
          <button onClick={copyCode} style={{ background:"none", border:"0.5px solid var(--color-border-secondary)", borderRadius:"var(--border-radius-md)", padding:"0.5rem 1rem", cursor:"pointer", color:"var(--color-text-secondary)", fontSize:"0.875rem" }}>
            {copied ? "✓ 복사됨" : "복사"}
          </button>
        </div>

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

        {error && <p style={{ color:"var(--color-text-danger)", fontSize:"0.875rem", margin:"0 0 1rem" }}>{error}</p>}

        {!wsReady && (
          <p style={{ color:"var(--color-text-tertiary)", fontSize:"0.85rem", textAlign:"center", marginBottom:"0.75rem" }}>서버에 연결 중...</p>
        )}

        {session.isHost ? (
          <button onClick={startGame} disabled={players.length < 2 || !wsReady} style={{
            width:"100%", background: players.length >= 2 && wsReady ? "var(--color-text-primary)" : "var(--color-background-secondary)",
            color: players.length >= 2 && wsReady ? "var(--color-background-primary)" : "var(--color-text-tertiary)",
            border:"none", padding:"0.875rem", borderRadius:"var(--border-radius-md)", fontSize:"1rem",
            fontWeight:500, cursor: players.length >= 2 && wsReady ? "pointer" : "not-allowed"
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
function GameScreen({ session, stompRef, wsReady, initState, onGameOver }) {
  const [currentTurnId, setCurrentTurnId] = useState(initState?.firstSessionId);
  const [currentNickname, setCurrentNickname] = useState(initState?.firstTurn);
  const [lastWord, setLastWord] = useState(null);
  const [requiredChar, setRequiredChar] = useState(null);
  const [wordInput, setWordInput] = useState("");
  const [wordHistory, setWordHistory] = useState([]);
  const [players, setPlayers] = useState(initState?.players || []);
  const [timer, setTimer] = useState(30);
  const [chatInput, setChatInput] = useState("");
  const [chatMessages, setChatMessages] = useState([]);
  const [error, setError] = useState("");
  const [pulse, setPulse] = useState(false);
  const chatEndRef = useRef(null);
  const isMyTurn = currentTurnId === session?.sessionId;

  // 시각적 카운트다운 (서버가 타임아웃 처리 담당, 클라이언트는 표시만)
  useEffect(() => {
    const interval = setInterval(() => {
      setTimer(prev => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  // WebSocket 구독
  useEffect(() => {
    if (!wsReady || !stompRef.current) return;
    const client = stompRef.current;

    const gameSub = client.subscribe(`/topic/room/${session.roomCode}/game`, (msg) => {
      const { type, payload } = JSON.parse(msg.body);
      switch (type) {
        case "WORD_SUBMITTED": {
          // payload: { word, submittedBy, nextTurn: { nextSessionId, nextNickname, requiredStartChar } }
          const { word, submittedBy, nextTurn } = payload;
          setLastWord(word);
          setRequiredChar(nextTurn.requiredStartChar);
          setWordHistory(prev => [...prev, { word, nickname: submittedBy }]);
          setCurrentTurnId(nextTurn.nextSessionId);
          setCurrentNickname(nextTurn.nextNickname);
          setTimer(30);
          setPulse(true);
          setTimeout(() => setPulse(false), 400);
          break;
        }
        case "TURN_CHANGED": {
          // payload: { nextSessionId, nextNickname, requiredStartChar }
          setCurrentTurnId(payload.nextSessionId);
          setCurrentNickname(payload.nextNickname);
          if (payload.requiredStartChar) setRequiredChar(payload.requiredStartChar);
          setTimer(30);
          break;
        }
        case "PLAYER_ELIMINATED": {
          // payload: { sessionId, nickname, reason }
          setPlayers(prev => prev.map(p =>
            p.sessionId === payload.sessionId ? { ...p, isAlive: false } : p
          ));
          break;
        }
        case "PLAYER_LEFT": {
          if (payload.players) setPlayers(payload.players);
          break;
        }
        case "GAME_OVER": {
          // payload: { winner, finalRanking, totalRounds }
          onGameOver(payload);
          break;
        }
      }
    });

    const chatSub = client.subscribe(`/topic/room/${session.roomCode}/chat`, (msg) => {
      const { type, payload } = JSON.parse(msg.body);
      if (type === "CHAT_MESSAGE") {
        const time = new Date(payload.timestamp).toLocaleTimeString("ko-KR", { hour:"2-digit", minute:"2-digit" });
        setChatMessages(prev => [...prev, { ...payload, time }]);
        setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior:"smooth" }), 50);
      }
    });

    // 개인 에러 채널 (단어 거절 메시지)
    const errorSub = client.subscribe("/user/queue/errors", (msg) => {
      const { type, payload } = JSON.parse(msg.body);
      if (type === "WORD_REJECTED") setError(payload.reason);
      else if (type === "ERROR") setError(payload.message);
    });

    // 채팅 히스토리 로드
    fetch(`${API_BASE}/rooms/${session.roomCode}/chat`)
      .then(r => r.json())
      .then(json => {
        if (json.success && json.data?.length) {
          setChatMessages(json.data.map(m => ({
            ...m,
            time: new Date(m.timestamp).toLocaleTimeString("ko-KR", { hour:"2-digit", minute:"2-digit" }),
          })));
        }
      })
      .catch(() => {});

    return () => {
      gameSub.unsubscribe();
      chatSub.unsubscribe();
      errorSub.unsubscribe();
    };
  }, [wsReady]);

  const submitWord = () => {
    if (!isMyTurn || !wordInput.trim()) return;
    setError("");
    stompRef.current.publish({
      destination: `/app/game/${session.roomCode}/submit-word`,
      body: JSON.stringify({ word: wordInput.trim(), sessionId: session.sessionId }),
    });
    setWordInput("");
  };

  const sendChat = () => {
    if (!chatInput.trim()) return;
    stompRef.current.publish({
      destination: `/app/chat/${session.roomCode}/send`,
      body: JSON.stringify({ content: chatInput.trim(), sessionId: session.sessionId }),
    });
    setChatInput("");
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
          <div style={{ height:6, background:"var(--color-background-secondary)", borderRadius:999, overflow:"hidden" }}>
            <div style={{ height:"100%", width:`${timerPct}%`, background: timerColor(timer), borderRadius:999, transition:"width 1s linear, background 0.3s" }} />
          </div>
        </div>

        {/* 이전 단어 표시 */}
        {lastWord ? (
          <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-lg)", border:"0.5px solid var(--color-border-tertiary)", padding:"1.25rem", textAlign:"center" }}>
            <p style={{ fontSize:"0.8rem", color:"var(--color-text-secondary)", margin:"0 0 8px" }}>이전 단어</p>
            <p style={{ fontSize:"2.5rem", fontWeight:700, margin:"0 0 8px", transition:"transform 0.3s", transform: pulse ? "scale(1.05)" : "scale(1)" }}>{lastWord}</p>
            <p style={{ fontSize:"1rem", color:"var(--color-text-secondary)", margin:0 }}>
              다음은 <strong style={{ color:"var(--color-text-primary)", fontSize:"1.3rem" }}>'{requiredChar}'</strong>으로 시작하는 단어
            </p>
          </div>
        ) : (
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
              <div key={p.sessionId} style={{ display:"flex", alignItems:"center", gap:"0.6rem", padding:"0.5rem 0.75rem", borderRadius:"var(--border-radius-md)", background: p.sessionId === currentTurnId ? "var(--color-background-info)" : "transparent", transition:"background 0.2s" }}>
                <div style={{ width:28, height:28, borderRadius:"50%", background: p.isAlive ? "var(--color-background-success)" : "var(--color-background-secondary)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:"0.75rem", fontWeight:600, color: p.isAlive ? "var(--color-text-success)" : "var(--color-text-tertiary)", flexShrink:0 }}>
                  {p.nickname[0]}
                </div>
                <span style={{ fontSize:"0.9rem", color: p.isAlive ? "var(--color-text-primary)" : "var(--color-text-tertiary)", textDecoration: p.isAlive ? "none" : "line-through" }}>{p.nickname}</span>
                {p.sessionId === currentTurnId && <span style={{ marginLeft:"auto", fontSize:"0.7rem", background:"var(--color-background-info)", color:"var(--color-text-info)", padding:"2px 6px", borderRadius:"999px" }}>턴</span>}
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
  // result: { winner: Player|null, finalRanking: Player[], totalRounds: Int }
  const winnerName = result?.winner?.nickname;

  return (
    <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", minHeight:"100vh", padding:"2rem" }}>
      <div style={{ background:"var(--color-background-primary)", borderRadius:"var(--border-radius-xl)", border:"0.5px solid var(--color-border-tertiary)", padding:"2.5rem", width:"100%", maxWidth:"480px", textAlign:"center" }}>
        <div style={{ fontSize:"4rem", marginBottom:"1rem" }}>🏆</div>
        <h2 style={{ fontSize:"1.8rem", fontWeight:700, margin:"0 0 0.5rem" }}>게임 종료!</h2>
        {winnerName ? (
          <p style={{ color:"var(--color-text-secondary)", marginBottom:"2rem" }}>
            <strong style={{ color:"var(--color-text-primary)", fontSize:"1.2rem" }}>{winnerName}</strong>님이 우승했습니다!
          </p>
        ) : (
          <p style={{ color:"var(--color-text-secondary)", marginBottom:"2rem" }}>무승부로 종료되었습니다.</p>
        )}

        <div style={{ background:"var(--color-background-secondary)", borderRadius:"var(--border-radius-lg)", padding:"1.25rem", marginBottom:"1rem", textAlign:"left" }}>
          <p style={{ fontSize:"0.85rem", color:"var(--color-text-secondary)", margin:"0 0 0.75rem" }}>
            최종 순위 · 총 {result?.totalRounds ?? 0}라운드
          </p>
          <div style={{ display:"flex", flexDirection:"column", gap:"0.5rem" }}>
            {result?.finalRanking?.map((p, i) => (
              <div key={p.sessionId} style={{ display:"flex", alignItems:"center", gap:"0.75rem", padding:"0.5rem 0.75rem", background:"var(--color-background-primary)", borderRadius:"var(--border-radius-md)" }}>
                <span style={{ fontSize:"1rem", fontWeight:700, color: i === 0 ? "#f59e0b" : "var(--color-text-tertiary)", width:20, textAlign:"center" }}>
                  {i + 1}
                </span>
                <div style={{ width:28, height:28, borderRadius:"50%", background:"var(--color-background-info)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:"0.75rem", fontWeight:600, color:"var(--color-text-info)" }}>
                  {p.nickname[0]}
                </div>
                <span style={{ fontWeight: i === 0 ? 600 : 400 }}>{p.nickname}</span>
                {i === 0 && <span style={{ marginLeft:"auto", fontSize:"0.75rem" }}>🏆</span>}
              </div>
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
