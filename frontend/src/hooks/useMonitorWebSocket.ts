'use client';

import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WebSocketPingPayload } from '@/types/monitor';

export function useMonitorWebSocket(onPingReceived: (payload: WebSocketPingPayload) => void) {
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        const socketUrl = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws';

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                client.subscribe('/topic/pings', (message) => {
                    if (message.body) {
                        try {
                            const data: WebSocketPingPayload = JSON.parse(message.body);
                            onPingReceived(data);
                        } catch (err) {
                            console.error('WebSocket veri ayrıştırma hatası:', err);
                        }
                    }
                });
            },
            onStompError: (frame) => {
                console.error('Broker hatası: ' + frame.headers['message']);
            },
        });

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
        };
    }, [onPingReceived]);
}