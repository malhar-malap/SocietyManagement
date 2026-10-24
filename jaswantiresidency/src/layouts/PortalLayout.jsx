import React, { useEffect, useState } from 'react';
import { Outlet, Navigate, NavLink, useLocation } from 'react-router-dom';
import { LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import NotificationBell from '../components/NotificationBell';
import logo from '../assets/logo.png';
import './layout.css';

const ROLE_LABELS = { MASTER_ADMIN: 'Master Administrator', ADMIN: 'Administrator', RESIDENT: 'Resident' };

/**
 * Shared shell for the admin and resident portals: sidebar navigation (an off-canvas drawer on small screens),
 * a top bar whose title follows the current page, real notifications, and sign-out.
 */
const PortalLayout = ({ portalName, navItems, titles, fallbackTitle }) => {
    const { logout, user } = useAuth();
    const location = useLocation();
    const [drawerOpen, setDrawerOpen] = useState(false);

    // Close the mobile drawer whenever the page changes
    useEffect(() => {
        setDrawerOpen(false);
    }, [location.pathname]);

    if (!user) return <Navigate to="/login" replace />;

    const title = Object.entries(titles).find(([path]) => location.pathname.startsWith(path))?.[1] || fallbackTitle;
    const visibleItems = navItems.filter((item) => !item.roles || item.roles.includes(user.role));

    return (
        <div className={`layout-container ${drawerOpen ? 'drawer-open' : ''}`}>
            <aside className="sidebar" id="portal-sidebar" aria-label={`${portalName} navigation`}>
                <div className="sidebar-brand">
                    <img src={logo} alt="" />
                    <span>
                        JASWANTI
                        <span className="brand-sub">{portalName}</span>
                    </span>
                    <button type="button" className="icon-btn sidebar-close" aria-label="Close menu" onClick={() => setDrawerOpen(false)}>
                        <X size={20} />
                    </button>
                </div>
                <nav className="sidebar-nav">
                    {visibleItems.map(({ to, label, icon: Icon }) => (
                        <NavLink key={`${to}-${label}`} to={to} className="nav-link">
                            <Icon size={18} aria-hidden="true" /> {label}
                        </NavLink>
                    ))}
                </nav>
                <div className="sidebar-footer">
                    <div className="sidebar-profile">
                        <div className="sidebar-avatar" aria-hidden="true">
                            {user.name ? user.name.charAt(0).toUpperCase() : '?'}
                        </div>
                        <div className="sidebar-profile-info">
                            <span className="sidebar-profile-name">{user.name || 'Signed in'}</span>
                            <span className="sidebar-profile-role">{ROLE_LABELS[user.role] || user.role}</span>
                        </div>
                    </div>
                </div>
            </aside>
            {drawerOpen && <div className="drawer-overlay" onClick={() => setDrawerOpen(false)} aria-hidden="true" />}
            <main className="main-content">
                <header className="topbar">
                    <div className="topbar-left">
                        <button
                            type="button"
                            className="icon-btn menu-toggle"
                            aria-label="Open menu"
                            aria-controls="portal-sidebar"
                            aria-expanded={drawerOpen}
                            onClick={() => setDrawerOpen(true)}
                        >
                            <Menu size={22} />
                        </button>
                        <h2 className="page-title">{title}</h2>
                    </div>
                    <div className="topbar-actions">
                        <NotificationBell userId={user.userId} />
                        <div className="profile-menu">
                            <button type="button" onClick={logout} className="logout-btn"><LogOut size={16}/> <span className="logout-label">Logout</span></button>
                        </div>
                    </div>
                </header>
                <div className="page-content">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default PortalLayout;
