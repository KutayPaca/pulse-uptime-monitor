import { Monitor, PingLog } from '@/types/monitor';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// Yeni eklenen istatistik arayüzü
export interface DatabaseStats {
    databaseSize: string;
    pingLogsTableSize: string;
    totalPingLogsCount: number;
}

export const monitorApi = {
    async getAll(): Promise<Monitor[]> {
        const res = await fetch(`${API_BASE}/monitors`, { cache: 'no-store' });
        if (!res.ok) throw new Error('Monitörler yüklenemedi');
        return res.json();
    },

    async create(data: { name: string; url: string; pingIntervalSeconds?: number }): Promise<Monitor> {
        const res = await fetch(`${API_BASE}/monitors`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });
        if (!res.ok) throw new Error('Monitör eklenemedi');
        return res.json();
    },

    async delete(id: number): Promise<void> {
        const res = await fetch(`${API_BASE}/monitors/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Monitör silinemedi');
    },

    async getLogs(id: number): Promise<PingLog[]> {
        const res = await fetch(`${API_BASE}/monitors/${id}/logs`, { cache: 'no-store' });
        if (!res.ok) throw new Error('Loglar alınamadı');
        return res.json();
    },

    // Yeni eklenen istatistikleri getiren metot
    async getStats(): Promise<DatabaseStats> {
        const res = await fetch(`${API_BASE}/monitors/stats`, { cache: 'no-store' });
        if (!res.ok) throw new Error('İstatistikler alınamadı');
        return res.json();
    },
};