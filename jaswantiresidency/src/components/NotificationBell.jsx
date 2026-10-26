import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, BellOff } from 'lucide-react';
import { getMyNotifications, markNotificationRead } from '../api/notificationApi';
import { EmptyState } from './ui/Feedback';

const POLL_MS = 60000;

// Broadcasts are one shared row on the server, so which ones this user has read is remembered in the browser
const readBroadcastKey = (userId) => `readBroadcasts:${userId}`;

const loadReadBroadcasts = (userId) => {
    try {
        return new Set(JSON.parse(localStorage.getItem(readBroadcastKey(userId)) || '[]'));
    } catch {
        return new Set();
    }
};

const saveReadBroadcasts = (userId, ids) => {
    try {
        localStorage.setItem(readBroadcastKey(userId), JSON.stringify([...ids]));
    } catch {
        /* storage unavailable: read state just won't persist */
    }
};

const timeAgo = (iso) => {
    if (!iso) return '';
    const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours} h ago`;
    return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
};

/** Real notifications for the signed-in user (it replaced a hard-coded "3" badge that did nothing). */
const NotificationBell = ({ userId }) => {
    const navigate = useNavigate();
    const [items, setItems] = useState([]);
    const [open, setOpen] = useState(false);
    const [readBroadcasts, setReadBroadcasts] = useState(() => loadReadBroadcasts(userId));
    const wrapperRef = useRef(null);

    const refresh = useCallback(async () => {
        try {
            setItems(await getMyNotifications());
        } catch {
            /* keep the last known list; the bell is not critical */
        }
    }, []);

    useEffect(() => {
        refresh();
        const timer = setInterval(() => {
            if (document.visibilityState === 'visible') refresh();
        }, POLL_MS);
        return () => clearInterval(timer);
    }, [refresh]);

    useEffect(() => {
        if (!open) return undefined;
        const onClick = (e) => { if (wrapperRef.current && !wrapperRef.current.contains(e.target)) setOpen(false); };
        const onKey = (e) => { if (e.key === 'Escape') setOpen(false); };
        document.addEventListener('mousedown', onClick);
        document.addEventListener('keydown', onKey);
        return () => {
            document.removeEventListener('mousedown', onClick);
            document.removeEventListener('keydown', onKey);
        };
    }, [open]);

    const isRead = (n) => (n.broadcast ? readBroadcasts.has(n.id) : n.read);
    const unread = items.filter((n) => !isRead(n)).length;

    const openItem = async (n) => {
        if (!isRead(n)) {
            if (n.broadcast) {
                const next = new Set(readBroadcasts).add(n.id);
                setReadBroadcasts(next);
                saveReadBroadcasts(userId, next);
            } else {
                setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
                markNotificationRead(n.id).catch(() => {});
            }
        }
        if (n.link) {
            setOpen(false);
            navigate(n.link);
        }
    };

    const markAllRead = () => {
        const broadcasts = new Set(readBroadcasts);
        items.forEach((n) => {
            if (isRead(n)) return;
            if (n.broadcast) broadcasts.add(n.id);
            else markNotificationRead(n.id).catch(() => {});
        });
        setReadBroadcasts(broadcasts);
        saveReadBroadcasts(userId, broadcasts);
        setItems((list) => list.map((x) => (x.broadcast ? x : { ...x, read: true })));
    };

    return (
        <div className="notification-bell" ref={wrapperRef}>
            <button
                type="button"
                className="icon-btn"
                aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
                aria-expanded={open}
                onClick={() => setOpen((o) => !o)}
            >
                <Bell size={19} />
                {unread > 0 && <span className="notification-badge">{unread > 9 ? '9+' : unread}</span>}
            </button>
            {open && (
                <div className="notification-panel" role="dialog" aria-label="Notifications">
                    <div className="notification-panel-header">
                        <h3>Notifications</h3>
                        {unread > 0 && <button type="button" className="btn btn-link btn-sm" onClick={markAllRead}>Mark all as read</button>}
                    </div>
                    {items.length === 0 ? (
                        <EmptyState icon={BellOff} title="You're all caught up" compact>New notices and updates will appear here.</EmptyState>
                    ) : (
                        <ul className="notification-list">
                            {items.slice(0, 20).map((n) => (
                                <li key={n.id}>
                                    <button type="button" className={`notification-item ${isRead(n) ? '' : 'unread'}`} onClick={() => openItem(n)}>
                                        <span className="dot" aria-hidden="true" />
                                        <span className="notification-title">{n.title}</span>
                                        <span className="notification-message">{n.message}</span>
                                        <span className="notification-time">{timeAgo(n.createdAt)}</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
        </div>
    );
};

export default NotificationBell;
