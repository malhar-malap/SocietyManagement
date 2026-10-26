import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/ui/Card';
import { Users, DollarSign, Activity, FileText, Home, Bell, TrendingUp, TrendingDown, Building2, Wallet, Banknote, AlertTriangle } from 'lucide-react';
import { XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, PieChart, Pie, Cell } from 'recharts';
import { useAuth } from '../context/AuthContext';
import { getAdminDashboard, getTransactions, getAllAnnouncements } from '../api/adminApi';
import './dashboard.css';
import './admin.css';

const STATUS_SLICES = [
    { key: 'PAID', name: 'Paid', color: '#10B981' },
    { key: 'PARTIALLY_PAID', name: 'Partly paid', color: '#F59E0B' },
    { key: 'PENDING', name: 'Pending', color: '#EF4444' },
];

const formatMoney = (value, fractionDigits = 0) =>
    `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits })}`;

const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
};

const monthLabel = (ym) => {
    if (!ym) return '';
    const [y, m] = ym.split('-').map(Number);
    return new Date(y, m - 1, 1).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
};

const AdminDashboard = () => {
    const { user } = useAuth();
    const [stats, setStats] = useState(null);
    const [recentTransactions, setRecentTransactions] = useState([]);
    const [latestAnnouncements, setLatestAnnouncements] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setIsLoading(true);
        setError('');
        try {
            // The headline figures are required; the two side lists degrade gracefully if they fail
            const [dashboard, transactions, announcements] = await Promise.allSettled([
                getAdminDashboard(), getTransactions(), getAllAnnouncements(),
            ]);
            if (dashboard.status !== 'fulfilled') throw dashboard.reason;
            setStats(dashboard.value);

            if (transactions.status === 'fulfilled') {
                setRecentTransactions([...(transactions.value || [])]
                    .sort((a, b) => new Date(b.timestamp || 0) - new Date(a.timestamp || 0))
                    .slice(0, 5));
            }
            if (announcements.status === 'fulfilled') {
                const now = Date.now();
                setLatestAnnouncements([...(announcements.value || [])]
                    .filter((a) => !a.expiryDate || new Date(a.expiryDate).getTime() > now)
                    .sort((a, b) => new Date(b.createdDate || 0) - new Date(a.createdDate || 0))
                    .slice(0, 3));
            }
        } catch (err) {
            // Never show placeholder figures on a finance dashboard: say it failed and offer a retry
            setError('Could not load the dashboard. Check your connection and try again.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    if (isLoading) {
        return (
            <div className="dashboard-wrapper" aria-busy="true">
                <div className="skeleton" style={{ height: 32, width: 280, marginBottom: '1.5rem' }} />
                <div className="dashboard-grid">
                    {Array.from({ length: 6 }).map((_, i) => <div key={i} className="skeleton" style={{ height: 110 }} />)}
                </div>
            </div>
        );
    }

    if (error || !stats) {
        return (
            <div className="dashboard-wrapper">
                <Card>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--danger)' }}>
                        <AlertTriangle size={20} aria-hidden="true" />
                        <span style={{ flex: 1 }}>{error || 'Dashboard data is unavailable.'}</span>
                        <button type="button" className="custom-btn btn-secondary" onClick={load}>Retry</button>
                    </div>
                </Card>
            </div>
        );
    }

    const collected = Number(stats.monthlyCollection || 0);
    const lastMonth = Number(stats.lastMonthCollection || 0);
    const collectionChange = lastMonth > 0 ? Math.round(((collected - lastMonth) / lastMonth) * 100) : null;

    const statusCounts = stats.billStatusThisMonth || {};
    const totalBillsThisMonth = STATUS_SLICES.reduce((sum, s) => sum + Number(statusCounts[s.key] || 0), 0);
    const pieData = STATUS_SLICES.map((s) => ({ ...s, value: Number(statusCounts[s.key] || 0) }));
    const trend = (stats.collectionTrend || []).map((p) => ({ ...p, amount: Number(p.amount || 0) }));
    const accounts = stats.bankAccounts || [];

    return (
        <div className="dashboard-wrapper animate-fade-up">

            {/* Hero Section */}
            <div className="dashboard-hero">
                <h1 className="hero-greeting">{greeting()}, {user?.name || 'Admin'} 👋</h1>
                <p className="hero-subtext">Here's what's happening at Jaswanti Residency today.</p>
            </div>

            {/* Live Society Bank Accounts & Cash Ledger */}
            <div style={{ marginTop: '1.5rem', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.75rem' }}>
                    <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--text-primary-light)', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <Building2 size={20} style={{ color: 'var(--primary-color)' }} />
                        Society Bank Accounts & Liquid Balances
                    </h3>
                </div>
                <div className="dashboard-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                    <Card className="stat-card animate-scale-in" style={{ borderLeft: '4px solid #10b981' }}>
                        <div className="stat-content">
                            <div className="stat-info">
                                <p className="stat-label">Total Liquid Funds</p>
                                <h2 className="stat-value" style={{ color: '#10b981' }}>{formatMoney(stats.totalLiquidBalance, 2)}</h2>
                                <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)', marginTop: '0.25rem' }}>Bank + Petty Cash</p>
                            </div>
                            <div className="stat-icon-wrapper" style={{ background: 'rgba(16,185,129,0.12)', color: '#10b981' }}>
                                <Wallet size={24} />
                            </div>
                        </div>
                    </Card>
                    {accounts.length === 0 ? (
                        <Card className="stat-card">
                            <p className="stat-label">No bank accounts set up yet</p>
                            <Link to="/admin/transactions" className="text-link" style={{ margin: 0, fontSize: '0.85rem' }}>Add one in Transactions &gt;</Link>
                        </Card>
                    ) : accounts.map((acc) => (
                        <Card key={acc.id} className="stat-card animate-scale-in" style={{ borderLeft: acc.accountType === 'CASH_LEDGER' ? '4px solid #f59e0b' : '4px solid #3b82f6' }}>
                            <div className="stat-content">
                                <div className="stat-info">
                                    <p className="stat-label">{acc.accountName}</p>
                                    <h2 className="stat-value">{formatMoney(acc.currentBalance, 2)}</h2>
                                    <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)', marginTop: '0.25rem' }}>
                                        {acc.accountType === 'BANK' ? `${acc.bankName || 'Bank'} Account` : 'Cash Ledger'}
                                    </p>
                                </div>
                                <div className="stat-icon-wrapper" style={{ background: acc.accountType === 'CASH_LEDGER' ? 'rgba(245,158,11,0.12)' : 'rgba(59,130,246,0.12)', color: acc.accountType === 'CASH_LEDGER' ? '#f59e0b' : '#3b82f6' }}>
                                    {acc.accountType === 'CASH_LEDGER' ? <Banknote size={24} /> : <Building2 size={24} />}
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            </div>

            <div className="dashboard-grid mt-4">

                {/* Stat Cards */}
                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.1s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Active Residents</p>
                            <h2 className="stat-value">{stats.totalResidents}</h2>
                            <p className="stat-trend">Accounts that can sign in</p>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <Users size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.2s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Collected This Month</p>
                            <h2 className="stat-value">{formatMoney(collected)}</h2>
                            {collectionChange === null ? (
                                <p className="stat-trend">Last month: {formatMoney(lastMonth)}</p>
                            ) : (
                                <p className={`stat-trend ${collectionChange >= 0 ? 'text-success' : 'text-danger'}`}>
                                    {collectionChange >= 0 ? <TrendingUp size={14}/> : <TrendingDown size={14}/>} {Math.abs(collectionChange)}% vs last month
                                </p>
                            )}
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <DollarSign size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.3s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Outstanding Dues</p>
                            <h2 className="stat-value">{formatMoney(stats.pendingAmount)}</h2>
                            <p className="stat-trend">Across {stats.unpaidResidents} unit{stats.unpaidResidents === 1 ? '' : 's'}</p>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <Activity size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.4s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Open Complaints</p>
                            <h2 className="stat-value">{stats.openComplaints}</h2>
                            <Link to="/admin/complaints" className="text-link" style={{margin:0, fontSize: '0.8rem'}}>Open helpdesk &gt;</Link>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <FileText size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.5s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Units With Dues</p>
                            <h2 className="stat-value">{stats.unpaidResidents}</h2>
                            <Link to="/admin/billing" className="text-link" style={{margin:0, fontSize: '0.8rem'}}>View details &gt;</Link>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <Home size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.6s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Active Announcements</p>
                            <h2 className="stat-value">{stats.activeAnnouncements}</h2>
                            <Link to="/admin/announcements" className="text-link" style={{margin:0, fontSize: '0.8rem'}}>View all &gt;</Link>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <Bell size={24} />
                        </div>
                    </div>
                </Card>
            </div>

            <div className="dashboard-grid mt-4">
                {/* Area Chart */}
                <Card className="col-span-2 glass" title="Collection Trend (Last 6 Months)">
                    <div style={{ height: '300px', marginTop: '1rem' }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={trend} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="var(--primary-color)" stopOpacity={0.3}/>
                                        <stop offset="95%" stopColor="var(--primary-color)" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary-light)', fontSize: 12}} />
                                <YAxis axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary-light)', fontSize: 12}}
                                       tickFormatter={(value) => (value >= 1000 ? `₹${Math.round(value / 1000)}k` : `₹${value}`)} />
                                <Tooltip
                                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: 'var(--shadow-md)' }}
                                    formatter={(value) => [formatMoney(value, 2), 'Collected']}
                                />
                                <Area type="monotone" dataKey="amount" stroke="var(--primary-color)" strokeWidth={3} fillOpacity={1} fill="url(#colorAmount)" />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </Card>

                {/* Doughnut Chart */}
                <Card className="glass" title={`Bill Status · ${monthLabel(stats.billingMonth)}`}>
                    {totalBillsThisMonth === 0 ? (
                        <p className="empty-state" style={{ padding: '2rem 0', textAlign: 'center', color: 'var(--text-secondary-light)' }}>
                            No bills have been generated for this month yet.
                        </p>
                    ) : (
                        <div style={{ height: '300px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie data={pieData} innerRadius={70} outerRadius={90} paddingAngle={5} dataKey="value">
                                        {pieData.map((entry) => <Cell key={entry.key} fill={entry.color} />)}
                                    </Pie>
                                    <Tooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: 'var(--shadow-md)' }} />
                                </PieChart>
                            </ResponsiveContainer>
                            <div className="pie-legend">
                                {pieData.map((entry) => (
                                    <div key={entry.key} className="pie-legend-item">
                                        <span className="pie-color" style={{backgroundColor: entry.color}}></span>
                                        <div className="pie-text">
                                            <strong>{entry.name}</strong>
                                            <span>{entry.value} ({Math.round((entry.value / totalBillsThisMonth) * 100)}%)</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </Card>
            </div>

            {/* Bottom Section: Recent Activity */}
            <div className="dashboard-grid mt-4">
                <Card className="col-span-2 glass" title="Recent Transactions" action={<Link to="/admin/transactions" className="text-link" style={{margin:0, fontSize: '0.85rem'}}>View All</Link>}>
                    {recentTransactions.length === 0 ? (
                        <p style={{ color: 'var(--text-secondary-light)', padding: '1rem 0' }}>No transactions recorded yet.</p>
                    ) : (
                        <div className="table-responsive">
                            <table className="recent-table">
                                <thead>
                                    <tr>
                                        <th>Unit / Payee</th>
                                        <th>Amount</th>
                                        <th>Type</th>
                                        <th>Date</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {recentTransactions.map((tx) => (
                                        <tr key={tx.id}>
                                            <td>{tx.roomNumber ? `${tx.roomNumber}${tx.residentName ? ` (${tx.residentName})` : ''}` : (tx.category || 'Society')}</td>
                                            <td>{formatMoney(tx.amount, 2)}</td>
                                            <td>
                                                <span className={tx.entryDirection === 'DEBIT' ? 'chip-warning' : (tx.status === 'SUCCESS' ? 'chip-success' : 'chip-default')}>
                                                    {tx.entryDirection === 'DEBIT' ? 'Expense' : (tx.status === 'SUCCESS' ? 'Received' : tx.status)}
                                                </span>
                                            </td>
                                            <td>{tx.paymentDate || (tx.timestamp ? new Date(tx.timestamp).toLocaleDateString('en-IN') : '')}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>

                <Card className="glass" title="Latest Announcements" action={<Link to="/admin/announcements" className="text-link" style={{margin:0, fontSize: '0.85rem'}}>View All</Link>}>
                    {latestAnnouncements.length === 0 ? (
                        <p style={{ color: 'var(--text-secondary-light)', padding: '1rem 0' }}>No active announcements.</p>
                    ) : (
                        <div className="recent-announcements">
                            {latestAnnouncements.map((a) => {
                                const created = a.createdDate ? new Date(a.createdDate) : null;
                                return (
                                    <div key={a.id} className="ann-card">
                                        <div className="ann-icon"><Bell size={16}/></div>
                                        <div className="ann-content">
                                            <h4>{a.title}</h4>
                                            <p>{a.description}</p>
                                        </div>
                                        {created && (
                                            <div className="ann-date">
                                                <strong>{created.getDate()}</strong>
                                                <span>{created.toLocaleDateString('en-IN', { month: 'short' }).toUpperCase()}</span>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default AdminDashboard;
