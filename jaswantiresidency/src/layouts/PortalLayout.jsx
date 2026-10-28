import React, { Suspense, useEffect, useRef, useState } from 'react';
import { Outlet, Navigate, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { ChevronDown, LogOut, Menu, UserRound, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import NotificationBell from '../components/NotificationBell';
import { HibiscusMark, PageLoader } from '../components/ui/Hibiscus';

const ROLE_LABELS = { MASTER_ADMIN: 'Master administrator', ADMIN: 'Administrator', OFFICE_ASSISTANT: 'Office assistant', RESIDENT: 'Resident' };

export const initials = (name) => (name || '?').trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join('').toUpperCase();

/**
 * Shared shell for the admin and resident portals: grouped sidebar navigation (an off-canvas drawer on small
 * screens), a quiet top bar with the page name, notifications and an account menu.
 */
const PortalLayout = ({ portalName, navGroups, titles, fallbackTitle, profilePath }) => {
    const { logout, user } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);
    const menuRef = useRef(null);

    useEffect(() => {
        setDrawerOpen(false);
        setMenuOpen(false);
        window.scrollTo(0, 0);
    }, [location.pathname]);

    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > 4);
        window.addEventListener('scroll', onScroll, { passive: true });
        return () => window.removeEventListener('scroll', onScroll);
    }, []);

    useEffect(() => {
        if (!menuOpen && !drawerOpen) return undefined;
        const onClick = (e) => { if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false); };
        const onKey = (e) => { if (e.key === 'Escape') { setMenuOpen(false); setDrawerOpen(false); } };
        document.addEventListener('mousedown', onClick);
        document.addEventListener('keydown', onKey);
        return () => { document.removeEventListener('mousedown', onClick); document.removeEventListener('keydown', onKey); };
    }, [menuOpen, drawerOpen]);

    if (!user) return <Navigate to="/login" replace />;

    const titleEntry = Object.entries(titles).find(([path]) => location.pathname.startsWith(path))?.[1];
    const title = (typeof titleEntry === 'function' ? titleEntry(user) : titleEntry) || fallbackTitle;
    const groups = navGroups
        .map((g) => ({ ...g, items: g.items.filter((item) => !item.roles || item.roles.includes(user.role)) }))
        .filter((g) => g.items.length > 0);
    const signOut = () => { logout(); navigate('/login', { replace: true }); };

    return (
        <div className={`shell ${drawerOpen ? 'drawer-open' : ''}`}>
            <a href="#main" className="visually-hidden">Skip to content</a>
            <aside className="sidebar" id="portal-sidebar" aria-label={`${portalName} navigation`}>
                <div className="sidebar-brand">
                    <HibiscusMark size={30} />
                    <div className="sidebar-brand-text">
                        <span className="sidebar-brand-name">Jaswanti</span>
                        <span className="sidebar-brand-sub">{portalName}</span>
                    </div>
                    <button type="button" className="icon-btn sidebar-close" aria-label="Close menu" onClick={() => setDrawerOpen(false)}>
                        <X size={20} />
                    </button>
                </div>
                <nav className="sidebar-nav">
                    {groups.map((group) => (
                        <div className="nav-group" key={group.label || 'main'}>
                            {group.label && <div className="nav-group-label">{group.label}</div>}
                            {group.items.map(({ to, label, icon: Icon }) => (
                                <NavLink key={`${to}-${label}`} to={to} className="nav-link">
                                    <Icon size={18} aria-hidden="true" strokeWidth={1.9} /> {label}
                                </NavLink>
                            ))}
                        </div>
                    ))}
                </nav>
                <div className="sidebar-footer">
                    <div className="sidebar-user">
                        <span className="avatar" aria-hidden="true">{initials(user.name)}</span>
                        <div className="sidebar-user-info">
                            <span className="sidebar-user-name truncate">{user.name || 'Signed in'}</span>
                            <span className="sidebar-user-role">{ROLE_LABELS[user.role] || user.role}</span>
                        </div>
                        <button type="button" className="icon-btn icon-btn-sm" onClick={signOut} aria-label="Sign out" title="Sign out">
                            <LogOut size={17} />
                        </button>
                    </div>
                </div>
            </aside>
            {drawerOpen && <div className="drawer-overlay" onClick={() => setDrawerOpen(false)} aria-hidden="true" />}

            <div className="main">
                <header className={`topbar ${scrolled ? 'is-scrolled' : ''}`}>
                    <div className="topbar-left">
                        <button type="button" className="icon-btn menu-toggle" aria-label="Open menu" aria-controls="portal-sidebar"
                            aria-expanded={drawerOpen} onClick={() => setDrawerOpen(true)}>
                            <Menu size={21} />
                        </button>
                        <span className="topbar-title"><strong>{title}</strong></span>
                    </div>
                    <div className="topbar-actions">
                        <NotificationBell userId={user.userId} />
                        <div className="user-menu" ref={menuRef}>
                            <button type="button" className="user-menu-trigger" aria-haspopup="menu" aria-expanded={menuOpen} onClick={() => setMenuOpen((o) => !o)}>
                                <span className="avatar avatar-sm" aria-hidden="true">{initials(user.name)}</span>
                                <span className="visually-hidden">Account menu</span>
                                <ChevronDown size={15} className="chevron text-subtle" aria-hidden="true" />
                            </button>
                            {menuOpen && (
                                <div className="menu" role="menu">
                                    <div className="menu-head">
                                        <div className="name">{user.name || 'Signed in'}</div>
                                        <div className="role">{ROLE_LABELS[user.role] || user.role}</div>
                                    </div>
                                    <div className="menu-sep" />
                                    {profilePath && (
                                        <button type="button" role="menuitem" className="menu-item" onClick={() => navigate(profilePath)}>
                                            <UserRound size={16} /> My profile
                                        </button>
                                    )}
                                    <button type="button" role="menuitem" className="menu-item danger" onClick={signOut}>
                                        <LogOut size={16} /> Sign out
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                </header>
                <main className="page page-enter" id="main" key={location.pathname}>
                    <Suspense fallback={<PageLoader label="Loading" />}>
                        <Outlet />
                    </Suspense>
                </main>
            </div>
        </div>
    );
};

export default PortalLayout;
