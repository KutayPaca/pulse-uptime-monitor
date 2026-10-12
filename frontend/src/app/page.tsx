'use client';

import { useEffect, useState, useCallback } from 'react';
import { Activity, Globe, Trash2, ArrowUpRight } from 'lucide-react';
import { Monitor, PingLog, WebSocketPingPayload } from '@/types/monitor';
import { monitorApi } from '@/lib/api';
import { useMonitorWebSocket } from '@/hooks/useMonitorWebSocket';
import { LatencyChart } from '@/components/LatencyChart';
import { AddMonitorModal } from '@/components/AddMonitorModal';

export default function Dashboard() {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [logs, setLogs] = useState<PingLog[]>([]);
  const [loading, setLoading] = useState(true);

  // İlk yükleme
  useEffect(() => {
    monitorApi
        .getAll()
        .then((data) => {
          setMonitors(data);
          if (data.length > 0) setSelectedId(data[0].id);
        })
        .catch((err) => console.error(err))
        .finally(() => setLoading(false));
  }, []);

  // Seçili monitörün loglarını getirme
  useEffect(() => {
    if (selectedId) {
      monitorApi.getLogs(selectedId).then(setLogs).catch(console.error);
    } else {
      setLogs([]);
    }
  }, [selectedId]);

  // Canlı WebSocket Ping dinleyicisi
  const handlePing = useCallback((payload: WebSocketPingPayload) => {
    setMonitors((prev) =>
        prev.map((m) => (m.id === payload.monitorId ? { ...m, status: payload.status } : m))
    );

    setSelectedId((currSelected) => {
      if (currSelected === payload.monitorId) {
        setLogs((prevLogs) => [
          {
            id: Date.now(),
            monitorId: payload.monitorId,
            statusCode: payload.statusCode,
            responseTimeMs: payload.responseTimeMs,
            status: payload.status,
            checkedAt: payload.checkedAt,
          },
          ...prevLogs.slice(0, 19),
        ]);
      }
      return currSelected;
    });
  }, []);

  useMonitorWebSocket(handlePing);

  const handleDelete = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    if (!confirm('Bu monitörü silmek istediğinize emin misiniz?')) return;

    try {
      await monitorApi.delete(id);
      setMonitors((prev) => prev.filter((m) => m.id !== id));
      if (selectedId === id) {
        const remaining = monitors.filter((m) => m.id !== id);
        setSelectedId(remaining.length > 0 ? remaining[0].id : null);
      }
    } catch (err) {
      alert('Silme işlemi başarısız');
    }
  };

  const selectedMonitor = monitors.find((m) => m.id === selectedId);
  const upCount = monitors.filter((m) => m.status === 'UP').length;
  const downCount = monitors.filter((m) => m.status === 'DOWN').length;

  return (
      <main className="min-h-screen bg-black text-zinc-100 p-6 md:p-10 font-sans">
        <div className="max-w-6xl mx-auto space-y-8">
          {/* Üst Bar */}
          <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-800 pb-6">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <Activity className="h-6 w-6" />
              </div>
              <div>
                <h1 className="text-xl font-bold tracking-tight text-white">Pulse Uptime</h1>
                <p className="text-xs text-zinc-400">Ağ gecikmesi ve servis izleme paneli</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <AddMonitorModal onCreated={(newMon) => setMonitors((prev) => [...prev, newMon])} />
            </div>
          </header>

          {/* İstatistik Kartları */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="p-4 rounded-xl border border-zinc-800/80 bg-zinc-900/40">
              <span className="text-xs text-zinc-400">Toplam İzlenen</span>
              <div className="text-2xl font-bold mt-1 text-white">{monitors.length}</div>
            </div>
            <div className="p-4 rounded-xl border border-emerald-950/60 bg-emerald-950/10">
              <span className="text-xs text-emerald-400">Aktif (UP)</span>
              <div className="text-2xl font-bold mt-1 text-emerald-400">{upCount}</div>
            </div>
            <div className="p-4 rounded-xl border border-rose-950/60 bg-rose-950/10">
              <span className="text-xs text-rose-400">Kesintide (DOWN)</span>
              <div className="text-2xl font-bold mt-1 text-rose-400">{downCount}</div>
            </div>
            <div className="p-4 rounded-xl border border-zinc-800/80 bg-zinc-900/40">
              <span className="text-xs text-zinc-400">Genel Sağlık</span>
              <div className="text-2xl font-bold mt-1 text-white">
                {monitors.length > 0 ? `${Math.round((upCount / monitors.length) * 100)}%` : '-%'}
              </div>
            </div>
          </div>

          {/* Ana İçerik: Liste ve Grafik */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Sol Kolon: Monitör Listesi */}
            <div className="space-y-3 lg:col-span-1">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Servisler</h2>
              {loading ? (
                  <div className="text-xs text-zinc-500 py-4">Monitörler yükleniyor...</div>
              ) : monitors.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-zinc-800 p-6 text-center text-xs text-zinc-500">
                    Henüz kayıtlı bir monitör yok. Yukarıdan ekleyin.
                  </div>
              ) : (
                  monitors.map((m) => (
                      <div
                          key={m.id}
                          onClick={() => setSelectedId(m.id)}
                          className={`group relative flex items-center justify-between p-3.5 rounded-xl border cursor-pointer transition ${
                              selectedId === m.id
                                  ? 'border-emerald-500/50 bg-zinc-900 shadow-sm'
                                  : 'border-zinc-800/80 bg-zinc-950/60 hover:border-zinc-700'
                          }`}
                      >
                        <div className="flex items-center gap-3 overflow-hidden">
                    <span
                        className={`h-2.5 w-2.5 shrink-0 rounded-full ${
                            m.status === 'UP'
                                ? 'bg-emerald-500 shadow-[0_0_8px_#10b981]'
                                : m.status === 'DOWN'
                                    ? 'bg-rose-500 shadow-[0_0_8px_#f43f5e]'
                                    : 'bg-amber-500'
                        }`}
                    />
                          <div className="overflow-hidden">
                            <div className="text-sm font-medium text-white truncate">{m.name}</div>
                            <div className="text-xs text-zinc-500 truncate">{m.url}</div>
                          </div>
                        </div>

                        <button
                            onClick={(e) => handleDelete(e, m.id)}
                            className="opacity-0 group-hover:opacity-100 p-1.5 text-zinc-500 hover:text-rose-400 transition"
                            title="Sil"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                  ))
              )}
            </div>

            {/* Sağ Kolon: Detay & Grafik */}
            <div className="space-y-4 lg:col-span-2">
              {selectedMonitor ? (
                  <>
                    <div className="flex items-center justify-between rounded-xl border border-zinc-800 bg-zinc-900/50 p-4">
                      <div>
                        <h3 className="text-base font-semibold text-white">{selectedMonitor.name}</h3>
                        <a
                            href={selectedMonitor.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center gap-1 text-xs text-zinc-400 hover:text-emerald-400 transition mt-0.5"
                        >
                          <Globe className="h-3.5 w-3.5" />
                          {selectedMonitor.url}
                          <ArrowUpRight className="h-3 w-3" />
                        </a>
                      </div>
                      <span
                          className={`px-2.5 py-1 rounded-full text-xs font-bold font-mono tracking-wide ${
                              selectedMonitor.status === 'UP'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : selectedMonitor.status === 'DOWN'
                                      ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                                      : 'bg-amber-500/10 text-amber-400'
                          }`}
                      >
                    {selectedMonitor.status}
                  </span>
                    </div>

                    <LatencyChart logs={logs} />
                  </>
              ) : (
                  <div className="flex h-64 items-center justify-center rounded-xl border border-zinc-800/60 bg-zinc-950/40 text-xs text-zinc-500">
                    Detayları görüntülemek için sol taraftan bir monitör seçin.
                  </div>
              )}
            </div>
          </div>
        </div>
      </main>
  );
}