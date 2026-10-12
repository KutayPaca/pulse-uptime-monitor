export interface Monitor {
    id: number;
    name: string;
    url: string;
    status: 'UP' | 'DOWN' | 'PENDING';
    pingIntervalSeconds: number;
    createdAt: string;
}

export interface PingLog {
    id: number;
    monitorId: number;
    statusCode: number;
    responseTimeMs: number;
    status: 'UP' | 'DOWN';
    checkedAt: string;
}

export interface WebSocketPingPayload {
    monitorId: number;
    status: 'UP' | 'DOWN';
    statusCode: number;
    responseTimeMs: number;
    checkedAt: string;
}