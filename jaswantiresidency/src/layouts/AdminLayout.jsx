import React from 'react';
import { LayoutDashboard, Building2, Users, Receipt, ArrowLeftRight, MessageSquare, Megaphone, ScrollText } from 'lucide-react';
import PortalLayout from './PortalLayout';

const NAV_ITEMS = [
    { to: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/admin/rooms', label: 'Rooms', icon: Building2 },
    { to: '/admin/residents', label: 'Residents', icon: Users, roles: ['ADMIN'] },
    { to: '/admin/residents', label: 'Users & Roles', icon: Users, roles: ['MASTER_ADMIN'] },
    { to: '/admin/billing', label: 'Billing', icon: Receipt },
    { to: '/admin/transactions', label: 'Transactions', icon: ArrowLeftRight },
    { to: '/admin/complaints', label: 'Complaints', icon: MessageSquare },
    { to: '/admin/announcements', label: 'Announcements', icon: Megaphone },
    { to: '/admin/system-logs', label: 'System Logs', icon: ScrollText, roles: ['MASTER_ADMIN'] },
];

const TITLES = {
    '/admin/dashboard': 'Dashboard',
    '/admin/rooms': 'Rooms & Occupancy',
    '/admin/residents': (u) => (u?.role === 'MASTER_ADMIN' ? 'Users & Roles' : 'Residents'),
    '/admin/users': (u) => (u?.role === 'MASTER_ADMIN' ? 'Users & Roles' : 'Residents'),
    '/admin/billing': 'Billing',
    '/admin/transactions': 'Transactions',
    '/admin/complaints': 'Helpdesk',
    '/admin/announcements': 'Announcements',
    '/admin/system-logs': 'System Logs',
};

const AdminLayout = () => (
    <PortalLayout portalName="Admin Portal" navItems={NAV_ITEMS} titles={TITLES} fallbackTitle="Admin Portal" />
);

export default AdminLayout;
