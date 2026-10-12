'use client';

import { useEffect, useState, useCallback } from 'react';
import { Activity, Globe, Trash2, ArrowUpRight, Clock, GripVertical } from 'lucide-react';
import { Monitor, PingLog, WebSocketPingPayload } from '@/types/monitor';
import { monitorApi, DatabaseStats, MonitorStats } from '@/lib/api';
import { useMonitorWebSocket } from '@/hooks/useMonitorWebSocket';
import { LatencyChart } from '@/components/LatencyChart';
import { AddMonitorModal } from '@/components/AddMonitorModal';

export default function Dashboard() {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [logs, setLogs] = useState<PingLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DatabaseStats | null>(null);
  const [monitorStats, setMonitorStats] = useState<MonitorStats | null>(null);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

  const fetchStats = () => {
    monitorApi.getStats().then(setStats).catch(console.error);
  };

  const saveOrderToStorage = (orderedMonitors: Monitor[]) => {
    try {
      localStorage.setItem('pulse_monitor_order', JSON.stringify(orderedMonitors.map((m) => m.id)));
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    let isMounted = true;
    monitorApi
        .getAll()
        .then((data) => {
          if (!isMounted) return;
          let sortedData = [...data];
          try {
            const savedOrder = localStorage.getItem('pulse_monitor_order');
            if (savedOrder) {
              const orderIds: number[] = JSON.parse(savedOrder);
              sortedData.sort((a, b) => {
                const idxA = orderIds.indexOf(a.id);
                const idxB = orderIds.indexOf(b.id);
                if (idxA === -1 && idxB === -1) return 0;
                if (idxA === -1) return 1;
                if (idxB === -1) return -1;
                return idxA - idxB;
              });
            }
          } catch (e) {
            console.error(e);
          }
          setMonitors(sortedData);
          if (sortedData.length > 0) setSelectedId(sortedData[0].id);
        })
        .catch(console.error)
        .finally(() => {
          if (isMounted) setLoading(false);
        });

    fetchStats();
    return () => { isMounted = false; };
  }, []);

  useEffect(() => {
    if (!selectedId) {
      setMonitorStats(null);
      return;
    }
    let isMounted = true;
    monitorApi.getLogs(selectedId).then((data) => { if (isMounted) setLogs(data); }).catch(console.error);
    monitorApi.getMonitorStats(selectedId).then((data) => { if (isMounted) setMonitorStats(data); }).catch(console.error);
    return () => { isMounted = false; };
  }, [selectedId]);

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
        monitorApi.getMonitorStats(payload.monitorId).then(setMonitorStats).catch(console.error);
      }
      return currSelected;
    });

    setStats((prev) =>
        prev ? { ...prev, totalPingLogsCount: prev.totalPingLogsCount + 1 } : prev
    );
  }, []);

  useMonitorWebSocket(handlePing);

  const handleDelete = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    if (!confirm('Bu monitörü silmek istediğinize emin misiniz?')) return;
    try {
      await monitorApi.delete(id);
      const updated = monitors.filter((m) => m.id !== id);
      setMonitors(updated);
      saveOrderToStorage(updated);
      if (selectedId === id) {
        setSelectedId(updated.length > 0 ? updated[0].id : null);
        setLogs([]);
        setMonitorStats(null);
      }
      fetchStats();
    } catch {
      alert('Silme işlemi başarısız');
    }
  };

  const handleDrop = (dropIndex: number) => {
    if (draggedIndex === null || draggedIndex === dropIndex) {
      setDraggedIndex(null);
      return;
    }
    const updated = [...monitors];
    const [movedItem] = updated.splice(draggedIndex, 1);
    updated.splice(dropIndex, 0, movedItem);
    setMonitors(updated);
    setDraggedIndex(null);
    saveOrderToStorage(updated);
  };

  const selectedMonitor = monitors.find((m) => m.id === selectedId);
  const upCount = monitors.filter((m) => m.status === 'UP').length;
  const downCount = monitors.filter((m) => m.status === 'DOWN').length;

  return (
      <main className="min-h-screen bg-[#09090b] text-zinc-100 p-6 md:p-10 font-sans selection:bg-emerald-500/20 selection:text-emerald-400">
        <div className="max-w-6xl mx-auto space-y-6">

          {/* Üst Bar: Minimal ve Temiz */}
          <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-2">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <Activity className="h-5 w-5" />
              </div>
              <div>
                <h1 className="text-lg font-semibold tracking-tight text-white">Pulse Uptime</h1>
                <p className="text-xs text-zinc-400">Servis durumu ve ağ gecikme paneli</p>
              </div>
            </div>
            <AddMonitorModal onCreated={(newMon) => {
              const updated = [...monitors, newMon];
              setMonitors(updated);
              saveOrderToStorage(updated);
              fetchStats();
            }} />
          </header>

          {/* Global Özet Bandı (Ağır 5 kart yerine hafif 4'lü kompakt bant) */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div className="rounded-xl border border-zinc-800/80 bg-zinc-900/30 p-3.5 flex items-center justify-between">
              <span className="text-xs text-zinc-400">İzlenen Servis</span>
              <span className="text-lg font-semibold text-white">{monitors.length}</span>
            </div>
            <div className="rounded-xl border border-zinc-800/80 bg-zinc-900/30 p-3.5 flex items-center justify-between">
              <span className="text-xs text-zinc-400">Çalışır Durumda</span>
              <span className="text-lg font-semibold text-emerald-400">{upCount} aktif</span>
            </div>
            <div className={`rounded-xl border p-3.5 flex items-center justify-between ${
                downCount > 0 ? 'border-rose-900/50 bg-rose-950/20' : 'border-zinc-800/80 bg-zinc-900/30'
            }`}>
              <span className="text-xs text-zinc-400">Kesintide</span>
              <span className={`text-lg font-semibold ${downCount > 0 ? 'text-rose-400' : 'text-zinc-500'}`}>
              {downCount}
            </span>
            </div>
            <div className="rounded-xl border border-zinc-800/80 bg-zinc-900/30 p-3.5 flex items-center justify-between">
              <span className="text-xs text-zinc-400">Veritabanı</span>
              <div className="text-right">
                <span className="text-xs font-mono text-zinc-300">{stats?.pingLogsTableSize || '0 kB'}</span>
                <span className="text-[10px] text-zinc-500 block">({stats?.totalPingLogsCount ?? 0} ping)</span>
              </div>
            </div>
          </div>

          {/* Ana Gövde */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 pt-2">

            {/* Sol Kolon: Servis Listesi */}
            <div className="space-y-2.5 lg:col-span-1">
              <div className="flex items-center justify-between px-1 pb-1">
                <h2 className="text-xs font-medium tracking-wider text-zinc-400 uppercase">Servisler</h2>
                <span className="text-[10px] text-zinc-600">Sürükle & Sırala</span>
              </div>

              {loading ? (
                  <div className="text-xs text-zinc-500 p-4">Yükleniyor...</div>
              ) : monitors.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-zinc-800/80 p-8 text-center text-xs text-zinc-500">
                    Kayıtlı monitör yok.
                  </div>
              ) : (
                  monitors.map((m, index) => (
                      <div
                          key={m.id}
                          draggable
                          onDragStart={() => setDraggedIndex(index)}
                          onDragOver={(e) => e.preventDefault()}
                          onDrop={() => handleDrop(index)}
                          onClick={() => setSelectedId(m.id)}
                          className={`group flex items-center justify-between p-3 rounded-xl border transition-all cursor-pointer ${
                              draggedIndex === index ? 'opacity-30 border-dashed border-emerald-500' : ''
                          } ${
                              selectedId === m.id
                                  ? 'border-emerald-500/40 bg-zinc-900/80 shadow-sm'
                                  : 'border-zinc-800/60 bg-zinc-900/20 hover:border-zinc-700/80 hover:bg-zinc-900/40'
                          }`}
                      >
                        <div className="flex items-center gap-2.5 overflow-hidden">
                          <GripVertical className="h-3.5 w-3.5 text-zinc-600 group-hover:text-zinc-400 shrink-0 cursor-grab" />
                          <span
                              className={`h-2 w-2 shrink-0 rounded-full ${
                                  m.status === 'UP'
                                      ? 'bg-emerald-500 shadow-[0_0_8px_#10b981]'
                                      : m.status === 'DOWN'
                                          ? 'bg-rose-500 shadow-[0_0_8px_#f43f5e]'
                                          : 'bg-amber-500'
                              }`}
                          />
                          <div className="overflow-hidden">
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-medium text-zinc-200 truncate">{m.name}</span>
                              <span className="text-[10px] font-mono text-zinc-500">
                          {m.pingIntervalSeconds ? `${m.pingIntervalSeconds}s` : '60s'}
                        </span>
                            </div>
                            <div className="text-xs text-zinc-500 truncate">{m.url}</div>
                          </div>
                        </div>
                        <button
                            onClick={(e) => handleDelete(e, m.id)}
                            className="opacity-0 group-hover:opacity-100 p-1.5 text-zinc-500 hover:text-rose-400 transition"
                            title="Sil"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                  ))
              )}
            </div>

            {/* Sağ Kolon: TEK BİR ENTEGRE PANEL (Tüm parçalanmış kutular burada birleşti) */}
            <div className="lg:col-span-2">
              {selectedMonitor ? (
                  <div className="rounded-2xl border border-zinc-800/80 bg-zinc-900/30 overflow-hidden">

                    {/* Entegre Panel Başlığı ve Hızlı Metrikler */}
                    <div className="p-5 border-b border-zinc-800/60 flex flex-col md:flex-row md:items-center justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-2.5">
                          <h3 className="text-base font-semibold text-white tracking-tight">{selectedMonitor.name}</h3>
                          <span className={`px-2 py-0.5 rounded-full text-[11px] font-mono font-medium ${
                              selectedMonitor.status === 'UP'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                          }`}>
                        {selectedMonitor.status}
                      </span>
                        </div>
                        <a
                            href={selectedMonitor.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1 text-xs text-zinc-400 hover:text-emerald-400 transition mt-1"
                        >
                          <Globe className="h-3 w-3" />
                          {selectedMonitor.url}
                          <ArrowUpRight className="h-3 w-3 text-zinc-500" />
                        </a>
                      </div>

                      {/* Yan Yana İki Temiz Metrik (Kutu değil, temiz tipografi) */}
                      <div className="flex items-center gap-6 bg-zinc-950/40 px-4 py-2 rounded-xl border border-zinc-800/50">
                        <div>
                          <span className="text-[10px] uppercase font-mono tracking-wider text-zinc-500 block">Uptime</span>
                          <span className="text-base font-semibold text-emerald-400">
                        %{monitorStats?.uptimePercentage || '0.00'}
                      </span>
                        </div>
                        <div className="h-7 w-px bg-zinc-800" />
                        <div>
                          <span className="text-[10px] uppercase font-mono tracking-wider text-zinc-500 block">Ortalama Yanıt</span>
                          <span className="text-base font-semibold text-zinc-200">
                        {monitorStats?.averageLatencyMs || 0} <span className="text-xs font-normal text-zinc-500">ms</span>
                      </span>
                        </div>
                      </div>
                    </div>

                    {/* Grafik (Doğrudan kartın gövdesine oturuyor) */}
                    <div className="p-4">
                      <LatencyChart logs={logs} />
                    </div>

                  </div>
              ) : (
                  <div className="h-64 rounded-2xl border border-dashed border-zinc-800/80 flex items-center justify-center text-xs text-zinc-500">
                    İncelemek için sol taraftan bir monitör seçin.
                  </div>
              )}
            </div>

          </div>
        </div>
      </main>
  );
}