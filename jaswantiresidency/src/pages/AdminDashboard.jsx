import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
    ArrowRight, ArrowUpRight, ArrowDownRight, Banknote, Building2, IndianRupee, Landmark, Megaphone,
    MessageSquare, Receipt, RefreshCw, Users, Wallet,
} from 'lucide-react';
import { XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid } from 'recharts';
import { useAuth } from '../context/AuthContext';
import { getAdminDashboard, getTransactions, getAllAnnouncements } from '../api/adminApi';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import { Alert, EmptyState, Skeleton, StatCard } from '../components/ui/Feedback';
import { firstName, formatDate, formatMoney, formatMoneyShort, greeting, monthLabel } from '../utils/format';

const STATUS_PARTS = [
    { key: 'PAID', name: 'Paid', tone: 'success' },
    { key: 'PARTIALLY_PAID', name: 'Partly paid', tone: 'warning' },
    { key: 'PENDING', name: 'Not paid', tone: 'danger' },
];

const ChartTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
        <div className="chart-tooltip">
            <div className="chart-tooltip-label">{label}</div>
            <div className="chart-tooltip-value">{formatMoney(payload[0].value)}</div>
        </div>
    );
};

const DashboardSkeleton = () => (
    <div aria-busy="true" aria-label="Loading overview">
        <Skeleton width={300} height={34} style={{ marginBottom: 10 }} />
        <Skeleton width={220} height={16} style={{ marginBottom: 28 }} />
        <div className="grid grid-4 mb-6">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} height={112} style={{ borderRadius: 14 }} />)}</div>
        <div className="dash-grid">
            <Skeleton height={340} style={{ borderRadius: 14 }} className="span-2" />
            <Skeleton height={340} style={{ borderRadius: 14 }} />
        </div>
    </div>
);

const AdminDashboard = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
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
                    .slice(0, 6));
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
            setError('We could not load the overview. Check your connection and try again.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    if (isLoading) return <DashboardSkeleton />;

    if (error || !stats) {
        return (
            <Alert tone="error" title="Overview unavailable" action={<Button variant="secondary" size="sm" icon={RefreshCw} onClick={load}>Try again</Button>}>
                {error || 'Dashboard data is unavailable.'}
            </Alert>
        );
    }

    const collected = Number(stats.monthlyCollection || 0);
    const lastMonth = Number(stats.lastMonthCollection || 0);
    const change = lastMonth > 0 ? Math.round(((collected - lastMonth) / lastMonth) * 100) : null;

    const statusCounts = stats.billStatusThisMonth || {};
    const parts = STATUS_PARTS.map((s) => ({ ...s, value: Number(statusCounts[s.key] || 0) }));
    const totalBills = parts.reduce((sum, p) => sum + p.value, 0);
    const paidShare = totalBills ? Math.round((parts[0].value / totalBills) * 100) : 0;
    const trend = (stats.collectionTrend || []).map((p) => ({ ...p, amount: Number(p.amount || 0) }));
    const accounts = stats.bankAccounts || [];
    const today = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' });

    return (
        <div className="stack stack-lg">
            <header className="page-header" style={{ marginBottom: 0 }}>
                <div className="grow">
                    <div className="eyebrow mb-2">{today}</div>
                    <h1>{greeting()}, {firstName(user?.name) || 'there'}</h1>
                    <p className="page-description">Here is how Jaswanti Residency is doing this month.</p>
                </div>
                <div className="header-actions">
                    {user?.role === 'MASTER_ADMIN' && <Button variant="secondary" icon={Megaphone} onClick={() => navigate('/admin/announcements')}>Post a notice</Button>}
                    <Button icon={Receipt} onClick={() => navigate('/admin/billing')}>Go to billing</Button>
                </div>
            </header>

            <div className="grid grid-4">
                <StatCard
                    label="Collected this month"
                    value={formatMoney(collected)}
                    icon={IndianRupee}
                    tone="success"
                    foot={change === null
                        ? <>Last month {formatMoney(lastMonth)}</>
                        : <><span className={change >= 0 ? 'up' : 'down'}>{change >= 0 ? <ArrowUpRight size={13} /> : <ArrowDownRight size={13} />}{Math.abs(change)}%</span> vs last month</>}
                />
                <StatCard
                    label="Outstanding dues"
                    value={formatMoney(stats.pendingAmount)}
                    icon={Wallet}
                    tone="warning"
                    foot={<>{stats.unpaidResidents} home{stats.unpaidResidents === 1 ? '' : 's'} with dues</>}
                    onClick={() => navigate('/admin/billing')}
                />
                <StatCard
                    label="Open requests"
                    value={stats.openComplaints}
                    icon={MessageSquare}
                    tone="brand"
                    foot={stats.openComplaints ? 'Waiting for a response' : 'Nothing waiting'}
                    onClick={() => navigate('/admin/complaints')}
                />
                <StatCard
                    label="Active residents"
                    value={stats.totalResidents}
                    icon={Users}
                    tone="info"
                    foot={<>{stats.activeAnnouncements} active notice{stats.activeAnnouncements === 1 ? '' : 's'}</>}
                    onClick={() => navigate('/admin/residents')}
                />
            </div>

            <div className="dash-grid">
                <Card className="span-2" title="Collections" subtitle="Payments received over the last six months">
                    {trend.length === 0 ? (
                        <EmptyState compact title="No collections yet">Payments will chart here once bills are paid.</EmptyState>
                    ) : (
                        <div className="chart-box">
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={trend} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="collectFill" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="0%" stopColor="#B4233C" stopOpacity={0.18} />
                                            <stop offset="100%" stopColor="#B4233C" stopOpacity={0} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid vertical={false} stroke="#ECEAE5" />
                                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#8A847C', fontSize: 12 }} dy={6} />
                                    <YAxis axisLine={false} tickLine={false} tick={{ fill: '#8A847C', fontSize: 12 }} tickFormatter={formatMoneyShort} width={56} />
                                    <Tooltip content={<ChartTooltip />} cursor={{ stroke: '#D6D3CC', strokeDasharray: '3 3' }} />
                                    <Area type="monotone" dataKey="amount" stroke="#B4233C" strokeWidth={2.25} fill="url(#collectFill)"
                                        activeDot={{ r: 4.5, strokeWidth: 2, stroke: '#fff', fill: '#B4233C' }} />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    )}
                </Card>

                <Card title="Society funds" subtitle="Live balances across accounts" action={<Link to="/admin/transactions" className="card-link">Ledger <ArrowRight size={14} /></Link>}>
                    <div className="funds-total">
                        <span className="text-muted text-sm">Total available</span>
                        <strong className="num">{formatMoney(stats.totalLiquidBalance, 2)}</strong>
                    </div>
                    {accounts.length === 0 ? (
                        <EmptyState compact icon={Landmark} title="No accounts yet" action={<Button size="sm" variant="secondary" onClick={() => navigate('/admin/transactions')}>Add an account</Button>}>
                            Add the society bank account to start tracking balances.
                        </EmptyState>
                    ) : (
                        <ul className="funds-list">
                            {accounts.map((acc) => {
                                const isCash = acc.accountType === 'CASH_LEDGER';
                                return (
                                    <li key={acc.id}>
                                        <span className={`stat-icon tone-${isCash ? 'warning' : 'info'}`}>{isCash ? <Banknote size={16} /> : <Building2 size={16} />}</span>
                                        <div className="grow">
                                            <div className="fw-600 truncate">{acc.accountName}</div>
                                            <div className="text-xs text-subtle">{isCash ? 'Petty cash' : `${acc.bankName || 'Bank'} account`}</div>
                                        </div>
                                        <span className="num fw-600">{formatMoney(acc.currentBalance, 2)}</span>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </Card>

                <Card title={`Bills for ${monthLabel(stats.billingMonth) || 'this month'}`} subtitle={totalBills ? `${totalBills} homes billed` : undefined}
                    action={<Link to="/admin/billing" className="card-link">Billing <ArrowRight size={14} /></Link>}>
                    {totalBills === 0 ? (
                        <EmptyState compact icon={Receipt} title="Not billed yet" action={<Button size="sm" onClick={() => navigate('/admin/billing')}>Generate bills</Button>}>
                            Generate this month’s maintenance bills to start collecting.
                        </EmptyState>
                    ) : (
                        <div className="stack">
                            <div>
                                <div className="row row-between mb-2"><span className="text-sm text-muted">Fully paid</span><strong className="num">{paidShare}%</strong></div>
                                <div className="stacked-bar" role="img" aria-label={parts.map((p) => `${p.name} ${p.value}`).join(', ')}>
                                    {parts.map((p) => p.value > 0 && <span key={p.key} className={`tone-${p.tone}`} style={{ flexGrow: p.value }} />)}
                                </div>
                            </div>
                            <ul className="legend-list">
                                {parts.map((p) => (
                                    <li key={p.key}>
                                        <span className={`legend-dot tone-${p.tone}`} />
                                        <span className="grow">{p.name}</span>
                                        <span className="num fw-600">{p.value}</span>
                                        <span className="num text-subtle legend-pct">{Math.round((p.value / totalBills) * 100)}%</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </Card>

                <Card className="span-2" title="Recent transactions" action={<Link to="/admin/transactions" className="card-link">View all <ArrowRight size={14} /></Link>} flush>
                    {recentTransactions.length === 0 ? (
                        <EmptyState compact title="No transactions yet">Payments and expenses will appear here as they are recorded.</EmptyState>
                    ) : (
                        <div className="table-scroll">
                            <table className="table table-compact">
                                <thead>
                                    <tr><th>Details</th><th>Date</th><th className="right">Amount</th></tr>
                                </thead>
                                <tbody>
                                    {recentTransactions.map((tx) => {
                                        const isDebit = tx.entryDirection === 'DEBIT';
                                        const hasRoom = !isDebit && tx.roomNumber && tx.roomNumber !== 'Unassigned';
                                        const resident = tx.residentName && tx.residentName !== 'Unknown' ? tx.residentName : '';
                                        return (
                                            <tr key={tx.id}>
                                                <td>
                                                    <div className="row" style={{ '--gap': '0.65rem' }}>
                                                        <span className={`tx-icon ${isDebit ? 'is-out' : 'is-in'}`}>{isDebit ? <ArrowUpRight size={15} /> : <ArrowDownRight size={15} />}</span>
                                                        <div className="grow">
                                                            <div className="cell-primary truncate">
                                                                {hasRoom ? `${tx.roomNumber}${resident ? ` · ${resident}` : ''}` : (tx.notes || (tx.category || 'Society expense').replace(/_/g, ' ').toLowerCase())}
                                                            </div>
                                                            <div className="cell-sub">{isDebit ? (tx.category || 'Expense').replace(/_/g, ' ').toLowerCase() : `Maintenance · ${(tx.paymentMode || '').replace(/_/g, ' ').toLowerCase()}`}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="text-muted nowrap">{formatDate(tx.paymentDate || tx.timestamp, { day: 'numeric', month: 'short' })}</td>
                                                <td className={`right num fw-600 ${isDebit ? '' : 'text-success'}`}>{isDebit ? '−' : '+'}{formatMoney(tx.amount, 2)}</td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>

                <Card className="span-all" title="Notice board" action={<Link to="/admin/announcements" className="card-link">All notices <ArrowRight size={14} /></Link>}>
                    {latestAnnouncements.length === 0 ? (
                        <EmptyState compact icon={Megaphone} title="No active notices" action={user?.role === 'MASTER_ADMIN' && <Button size="sm" variant="secondary" onClick={() => navigate('/admin/announcements')}>Post a notice</Button>}>
                            Notices you post appear on every resident’s home screen.
                        </EmptyState>
                    ) : (
                        <div className="notice-grid">
                            {latestAnnouncements.map((a) => (
                                <article key={a.id} className="notice">
                                    <div className="notice-date">{formatDate(a.createdDate, { day: 'numeric', month: 'short' })}</div>
                                    <h4>{a.title}</h4>
                                    <p>{a.description}</p>
                                </article>
                            ))}
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default AdminDashboard;
