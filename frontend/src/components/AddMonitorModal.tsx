'use client';

import { useState } from 'react';
import { Plus, X } from 'lucide-react';
import { monitorApi } from '@/lib/api';
import { Monitor } from '@/types/monitor';

export function AddMonitorModal({ onCreated }: { onCreated: (monitor: Monitor) => void }) {
    const [isOpen, setIsOpen] = useState(false);
    const [name, setName] = useState('');
    const [url, setUrl] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name || !url) return;

        setLoading(true);
        try {
            const created = await monitorApi.create({ name, url });
            onCreated(created);
            setName('');
            setUrl('');
            setIsOpen(false);
        } catch (err) {
            alert('Monitör eklenirken bir hata oluştu');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <button
                onClick={() => setIsOpen(true)}
                className="flex items-center gap-2 rounded-lg bg-emerald-600 px-3.5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-emerald-500 transition"
            >
                <Plus className="h-4 w-4" /> Yeni Monitör Ekle
            </button>

            {isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
                    <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-6 shadow-xl">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-base font-semibold text-white">Yeni Web Sitesi / API İzle</h3>
                            <button
                                onClick={() => setIsOpen(false)}
                                className="text-zinc-400 hover:text-white transition"
                            >
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-xs font-medium text-zinc-300 mb-1">Monitör Adı</label>
                                <input
                                    type="text"
                                    placeholder="Örn: Prod API, Google vb."
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-3 py-2 text-sm text-white placeholder-zinc-500 focus:border-emerald-500 focus:outline-none"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-xs font-medium text-zinc-300 mb-1">Hedef URL</label>
                                <input
                                    type="url"
                                    placeholder="https://api.ornek.com/health"
                                    value={url}
                                    onChange={(e) => setUrl(e.target.value)}
                                    className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-3 py-2 text-sm text-white placeholder-zinc-500 focus:border-emerald-500 focus:outline-none"
                                    required
                                />
                            </div>

                            <div className="flex justify-end gap-2 pt-2">
                                <button
                                    type="button"
                                    onClick={() => setIsOpen(false)}
                                    className="rounded-lg px-3.5 py-2 text-xs font-medium text-zinc-400 hover:bg-zinc-800 transition"
                                >
                                    İptal
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="rounded-lg bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50 transition"
                                >
                                    {loading ? 'Ekleniyor...' : 'Kaydet'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </>
    );
}