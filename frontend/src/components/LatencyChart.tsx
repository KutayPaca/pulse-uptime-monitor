'use client';

import {
    ResponsiveContainer,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
} from 'recharts';
import { PingLog } from '@/types/monitor';

export function LatencyChart({ logs }: { logs: PingLog[] }) {
    const chartData = [...logs]
        .reverse()
        .map((log) => ({
            time: new Date(log.checkedAt).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
            }),
            latency: log.responseTimeMs,
            status: log.status,
            statusCode: log.statusCode,
        }));

    if (chartData.length === 0) {
        return (
            <div className="flex h-56 items-center justify-center rounded-xl border border-zinc-800 bg-zinc-950/50 text-sm text-zinc-500">
                Henüz ping verisi bulunmuyor.
            </div>
        );
    }

    return (
        <div className="h-64 w-full rounded-xl border border-zinc-800 bg-zinc-950/60 p-4">
            <div className="mb-2 flex items-center justify-between">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                    Gecikme Süresi (ms)
                </h4>
                <span className="text-xs text-zinc-500">Son {chartData.length} Kontrol</span>
            </div>
            <ResponsiveContainer width="100%" height="88%">
                <AreaChart data={chartData}>
                    <defs>
                        <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#10b981" stopOpacity={0.35} />
                            <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                        </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#27272a" opacity={0.6} />
                    <XAxis dataKey="time" stroke="#71717a" fontSize={11} tickLine={false} />
                    <YAxis stroke="#71717a" fontSize={11} tickLine={false} domain={[0, 'auto']} />
                    <Tooltip
                        contentStyle={{
                            backgroundColor: '#18181b',
                            borderColor: '#27272a',
                            borderRadius: '8px',
                            fontSize: '12px',
                        }}
                        formatter={(val: any) => [`${val} ms`, 'Gecikme']}
                        labelFormatter={(label) => `Saat: ${label}`}
                    />
                    <Area
                        type="monotone"
                        dataKey="latency"
                        stroke="#10b981"
                        strokeWidth={2}
                        fillOpacity={1}
                        fill="url(#latencyGradient)"
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    );
}