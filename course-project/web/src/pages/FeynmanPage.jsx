import { useEffect, useRef, useState } from 'react';
import { CheckCircle2, Mic2, Play, RotateCcw, Send, Square, Sparkles } from 'lucide-react';
import { apiRequest } from '../lib/api.js';
import { convertToWav } from '../lib/audio.js';
import PageLoading from '../components/PageLoading.jsx';
import { cachedRequest, invalidateApiCache } from '../lib/dataCache.js';

const createAttemptId = () => globalThis.crypto?.randomUUID?.()
  || `attempt-${Date.now()}-${Math.random().toString(36).slice(2)}`;
const MAX_RECORDING_SECONDS = 60;

export default function FeynmanPage() {
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [audioUrl, setAudioUrl] = useState('');
  const [transcript, setTranscript] = useState('');
  const [evaluation, setEvaluation] = useState(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [loadingItems, setLoadingItems] = useState(true);
  const recorderRef = useRef(null);
  const streamRef = useRef(null);
  const recordingTimerRef = useRef(null);
  const mountedRef = useRef(true);
  const attemptIdRef = useRef('');
  const chunksRef = useRef([]);

  useEffect(() => {
    cachedRequest('knowledge', '/knowledge')
      .then(({ items: nextItems }) => {
        setItems(nextItems);
        if (nextItems[0]) setSelectedId(nextItems[0].id);
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoadingItems(false));
  }, []);

  useEffect(() => () => {
    if (audioUrl) URL.revokeObjectURL(audioUrl);
  }, [audioUrl]);

  const releaseStream = () => {
    const stream = streamRef.current;
    streamRef.current = null;
    stream?.getTracks().forEach((track) => track.stop());
  };

  const clearRecordingTimer = () => {
    if (recordingTimerRef.current) {
      window.clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }
  };

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      const recorder = recorderRef.current;
      recorderRef.current = null;
      if (recorder && recorder.state !== 'inactive') recorder.stop();
      clearRecordingTimer();
      releaseStream();
    };
  }, []);

  const startRecording = async () => {
    setError('');
    setEvaluation(null);
    setTranscript('');
    setSaved(false);
    setAudioUrl('');
    attemptIdRef.current = createAttemptId();
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      setError('当前浏览器不支持录音，请使用最新版 Chrome 或 Edge。');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      const recorder = new MediaRecorder(stream);
      chunksRef.current = [];
      recorder.ondataavailable = (event) => event.data.size && chunksRef.current.push(event.data);
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        clearRecordingTimer();
        if (mountedRef.current) {
          setAudioUrl(URL.createObjectURL(blob));
          setIsRecording(false);
          setRecordingSeconds(0);
        }
        releaseStream();
      };
      recorder.onerror = () => {
        clearRecordingTimer();
        if (mountedRef.current) {
          setError('录音过程中发生错误，请重试。');
          setIsRecording(false);
          setRecordingSeconds(0);
        }
        releaseStream();
        recorderRef.current = null;
      };
      recorder.start();
      recorderRef.current = recorder;
      setIsRecording(true);
      setRecordingSeconds(0);
      recordingTimerRef.current = window.setInterval(() => {
        setRecordingSeconds((seconds) => {
          const nextSeconds = seconds + 1;
          if (nextSeconds >= MAX_RECORDING_SECONDS) window.setTimeout(stopRecording, 0);
          return Math.min(nextSeconds, MAX_RECORDING_SECONDS);
        });
      }, 1000);
    } catch (_error) {
      clearRecordingTimer();
      releaseStream();
      recorderRef.current = null;
      setError('没有获得麦克风权限，请允许浏览器使用麦克风后重试。');
    }
  };

  const stopRecording = () => {
    const recorder = recorderRef.current;
    recorderRef.current = null;
    clearRecordingTimer();
    setIsRecording(false);
    setRecordingSeconds(0);
    if (recorder && recorder.state !== 'inactive') {
      recorder.stop();
    } else {
      releaseStream();
    }
  };

  const transcribe = async () => {
    if (!audioUrl || !selectedId) return;
    setBusy(true);
    setError('');
    try {
      const response = await fetch(audioUrl);
      const recordedBlob = await response.blob();
      const wavBlob = await convertToWav(recordedBlob);
      const formData = new FormData();
      formData.append('knowledgeId', selectedId);
      formData.append('audio', wavBlob, 'feynman-rehearsal.wav');
      const result = await apiRequest('/feynman/transcribe', { method: 'POST', body: formData });
      setTranscript(result.transcript);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const evaluate = async () => {
    if (!transcript || !selectedId) return;
    setBusy(true);
    setError('');
    try {
      const result = await apiRequest('/feynman/evaluate', {
        method: 'POST',
        body: JSON.stringify({ knowledgeId: selectedId, transcript })
      });
      setEvaluation(result);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const saveAttempt = async () => {
    if (!evaluation || saved) return;
    setBusy(true);
    setError('');
    try {
      await apiRequest('/feynman/attempts', {
        method: 'POST',
        body: JSON.stringify({ knowledgeId: selectedId, transcript, clientAttemptId: attemptIdRef.current, ...evaluation })
      });
      setSaved(true);
      invalidateApiCache('knowledge', 'knowledge-graph');
      setError('复述记录已保存，知识点掌握度已更新。');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const reset = () => {
    setAudioUrl('');
    setTranscript('');
    setEvaluation(null);
    setSaved(false);
    setError('');
    attemptIdRef.current = '';
  };

  return (
    <div className="page-wrap">
      <header className="page-header">
        <div><p className="eyebrow">Feynman loop / 02</p><h1>讲给自己听。</h1><p className="page-subtitle">不看笔记，试着把一个概念讲清楚。</p></div>
        <div className="feynman-mark"><Mic2 size={18} /><span>录音只在本次会话中处理</span></div>
      </header>
      {error && <div className={`inline-message ${error.includes('已保存') ? 'success' : ''}`}>{error}</div>}
      {loadingItems ? <PageLoading label="正在准备费曼复述…" /> : items.length === 0 ? <div className="list-empty"><Sparkles size={22} /><h2>先创建一个知识点</h2><p>选择一个你想重新讲清楚的概念。</p></div> : <div className="feynman-layout">
        <section className="feynman-card rehearsal-card">
          <div className="card-kicker"><span>01 / 选择对象</span><span>{items.length} 个知识点</span></div>
          <label className="select-label">今天要讲什么？<select value={selectedId} disabled={isRecording || busy} onChange={(event) => { if (isRecording || busy) return; setSelectedId(event.target.value); reset(); }}><option value="" disabled>选择知识点</option>{items.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}</select></label>
          <div className="rehearsal-stage"><div className={`record-orb ${isRecording ? 'recording' : ''}`}><Mic2 size={34} /></div><p className="stage-label">{isRecording ? `正在聆听… ${recordingSeconds}s / ${MAX_RECORDING_SECONDS}s` : audioUrl ? '录音完成' : '准备好之后开始'}</p><span>百度短语音识别单段最多 60 秒，尽量在一分钟内讲清核心概念并补充一个例子。</span></div>
          <div className="record-actions">{isRecording ? <button className="record-button stop" type="button" onClick={stopRecording}><Square size={16} /> 停止录音</button> : <button className="record-button" type="button" onClick={startRecording} disabled={!selectedId}><Mic2 size={17} /> 开始录音</button>}{audioUrl && <><audio controls src={audioUrl} /><button className="icon-button" type="button" onClick={reset} aria-label="重新录音"><RotateCcw size={17} /></button></>}</div>
          {audioUrl && !transcript && <button className="primary-button full-button" type="button" onClick={transcribe} disabled={busy}><Send size={16} />{busy ? '转录中…' : '上传并转成文字'}</button>}
        </section>
        <section className="feynman-card feedback-card">
          <div className="card-kicker"><span>02 / 复述与校准</span><span className="provider-note"><Sparkles size={14} /> {evaluation?.provider || 'Provider ready'}</span></div>
          {!transcript ? <div className="feedback-empty"><Play size={19} /><p>完成录音后，这里会出现你的复述文本。</p></div> : <><label className="transcript-label">你的复述<textarea value={transcript} onChange={(event) => setTranscript(event.target.value)} rows={7} /></label>{!evaluation && <button className="primary-button full-button" type="button" onClick={evaluate} disabled={busy}>{busy ? '评价中…' : '开始 AI 评价'}<Sparkles size={16} /></button>}{evaluation && <div className="evaluation-result"><div className="score-line"><div><span>本次掌握度</span><strong>{evaluation.score}</strong><small>/ 100</small></div><CheckCircle2 size={24} /></div><p className="evaluation-copy">{evaluation.evaluation}</p><div className="feedback-columns"><div><span>做得好的地方</span>{evaluation.strengths.map((item) => <p key={item}>+ {item}</p>)}</div><div><span>下一次试试</span>{evaluation.weaknesses.map((item) => <p key={item}>→ {item}</p>)}</div></div><button className="outline-button full-button" type="button" onClick={saveAttempt} disabled={busy || saved}>{saved ? '已保存' : '保存这次复述'} <CheckCircle2 size={16} /></button></div>}</>}
        </section>
      </div>}
    </div>
  );
}
