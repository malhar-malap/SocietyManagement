import React, { useState, useEffect } from 'react';
import Card from '../components/ui/Card';
import { Users, DollarSign, Activity, FileText, UserX, Bell, TrendingUp, TrendingDown, Building2, Wallet, Banknote } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area, PieChart, Pie, Cell } from 'recharts';
import './dashboard.css';
import './admin.css';

const AdminDashboard = () => {
    const [stats, setStats] = useState({
        totalResidents: 0,
        monthlyCollection: 0,
        pendingAmount: 0,
        openComplaints: 0,
        unpaidResidents: 0,
        activeAnnouncements: 0
    });
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchDashboard = async () => {
            try {
                const { getAdminDashboard } = await import('../api/adminApi');
                const data = await getAdminDashboard();
                setStats(data);
            } catch (err) {
                // Fallback to stub
                setStats({
                    totalResidents: 142,
                    monthlyCollection: 245000,
                    pendingAmount: 32000,
                    openComplaints: 8,
                    unpaidResidents: 18,
                    activeAnnouncements: 5
                });
            } finally {
                setIsLoading(false);
            }
        };
        fetchDashboard();
    }, []);

    const lineData = [
        { name: 'Jan', amount: 210000 },
        { name: 'Feb', amount: 225000 },
        { name: 'Mar', amount: 230000 },
        { name: 'Apr', amount: 215000 },
        { name: 'May', amount: 240000 },
        { name: 'Jun', amount: 245000 }
    ];

    const pieData = [
        { name: 'Paid', value: 62, color: '#10B981' }, // Green
        { name: 'Pending', value: 28, color: '#EF4444' }, // Red
        { name: 'Overdue', value: 10, color: '#F59E0B' } // Orange
    ];

    if (isLoading) return <div style={{ padding: '2rem' }}>Loading dashboard...</div>;

    return (
        <div className="dashboard-wrapper animate-fade-up">
            
            {/* Hero Section */}
            <div className="dashboard-hero">
                <h1 className="hero-greeting">Good morning, Admin 👋</h1>
                <p className="hero-subtext">Here's what's happening at Jaswanti Residency today.</p>
            </div>

            {/* Live Society Bank Accounts & Cash Ledger */}
            <div style={{ marginTop: '1.5rem', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                    <h3 style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--text-primary-light)', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <Building2 size={20} style={{ color: 'var(--primary-color)' }} />
                        Society Bank Accounts & Liquid Balances
                    </h3>
                    <span style={{ fontSize: '0.75rem', color: '#10b981', background: 'rgba(16,185,129,0.1)', padding: '0.2rem 0.6rem', borderRadius: 20, fontWeight: 600 }}>
                        ● Live Running Balances
                    </span>
                </div>
                <div className="dashboard-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                    <Card className="stat-card animate-scale-in" style={{ borderLeft: '4px solid #10b981' }}>
                        <div className="stat-content">
                            <div className="stat-info">
                                <p className="stat-label">Total Liquid Funds</p>
                                <h2 className="stat-value" style={{ color: '#10b981' }}>
                                    ₹{Number(stats.totalLiquidBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </h2>
                                <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)', marginTop: '0.25rem' }}>Bank + Petty Cash</p>
                            </div>
                            <div className="stat-icon-wrapper" style={{ background: 'rgba(16,185,129,0.12)', color: '#10b981' }}>
                                <Wallet size={24} />
                            </div>
                        </div>
                    </Card>
                    {(stats.bankAccounts && stats.bankAccounts.length > 0 ? stats.bankAccounts : [
                        { id: 1, accountName: 'Main Maintenance Bank A/c', accountType: 'BANK', bankName: 'HDFC Bank', currentBalance: 0 },
                        { id: 2, accountName: 'Petty Cash Ledger', accountType: 'CASH_LEDGER', currentBalance: 0 }
                    ]).map((acc, idx) => (
                        <Card key={acc.id || idx} className="stat-card animate-scale-in" style={{ borderLeft: acc.accountType === 'CASH_LEDGER' ? '4px solid #f59e0b' : '4px solid #3b82f6' }}>
                            <div className="stat-content">
                                <div className="stat-info">
                                    <p className="stat-label">{acc.accountName}</p>
                                    <h2 className="stat-value">
                                        ₹{Number(acc.currentBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                    </h2>
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
                            <p className="stat-label">Total Residents</p>
                            <h2 className="stat-value">{stats.totalResidents}</h2>
                            <p className="stat-trend text-success"><TrendingUp size={14}/> 6 this month</p>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <Users size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.2s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Monthly Collection</p>
                            <h2 className="stat-value">₹{stats.monthlyCollection.toLocaleString()}</h2>
                            <p className="stat-trend text-success"><TrendingUp size={14}/> 18% from last month</p>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <DollarSign size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.3s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Pending Dues</p>
                            <h2 className="stat-value">₹{stats.pendingAmount.toLocaleString()}</h2>
                            <p className="stat-trend text-danger"><TrendingDown size={14}/> 5% from last month</p>
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
                            <p className="stat-trend text-success"><TrendingDown size={14}/> 2 from last week</p>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <FileText size={24} />
                        </div>
                    </div>
                </Card>
                
                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.5s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Unpaid Residents</p>
                            <h2 className="stat-value">{stats.unpaidResidents}</h2>
                            <a href="/admin/billing" className="text-link" style={{margin:0, fontSize: '0.8rem'}}>View details &gt;</a>
                        </div>
                        <div className="stat-icon-wrapper" style={{background: 'rgba(225,29,72,0.1)', color: 'var(--primary-color)'}}>
                            <UserX size={24} />
                        </div>
                    </div>
                </Card>

                <Card className="stat-card animate-scale-in" style={{animationDelay: '0.6s'}}>
                    <div className="stat-content">
                        <div className="stat-info">
                            <p className="stat-label">Active Announcements</p>
                            <h2 className="stat-value">{stats.activeAnnouncements}</h2>
                            <a href="/admin/announcements" className="text-link" style={{margin:0, fontSize: '0.8rem'}}>View all &gt;</a>
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
                            <AreaChart data={lineData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="var(--primary-color)" stopOpacity={0.3}/>
                                        <stop offset="95%" stopColor="var(--primary-color)" stopOpacity={0}/>
                                    </linearGradient>
                                </defs>
                                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary-light)', fontSize: 12}} />
                                <YAxis axisLine={false} tickLine={false} tick={{fill: 'var(--text-secondary-light)', fontSize: 12}} tickFormatter={(value) => `₹${value/1000}k`} />
                                <Tooltip 
                                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: 'var(--shadow-md)' }}
                                    formatter={(value) => [`₹${value.toLocaleString()}`, 'Amount']}
                                />
                                <Area type="monotone" dataKey="amount" stroke="var(--primary-color)" strokeWidth={3} fillOpacity={1} fill="url(#colorAmount)" />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </Card>

                {/* Doughnut Chart */}
                <Card className="glass" title="Payment Status Overview">
                    <div style={{ height: '300px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={pieData}
                                    innerRadius={70}
                                    outerRadius={90}
                                    paddingAngle={5}
                                    dataKey="value"
                                >
                                    {pieData.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={entry.color} />
                                    ))}
                                </Pie>
                                <Tooltip 
                                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: 'var(--shadow-md)' }}
                                />
                            </PieChart>
                        </ResponsiveContainer>
                        <div className="pie-legend">
                            {pieData.map((entry, index) => (
                                <div key={index} className="pie-legend-item">
                                    <span className="pie-color" style={{backgroundColor: entry.color}}></span>
                                    <div className="pie-text">
                                        <strong>{entry.name}</strong>
                                        <span>{entry.value}%</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </Card>
            </div>

            {/* Bottom Section: Recent Activity */}
            <div className="dashboard-grid mt-4">
                <Card className="col-span-2 glass" title="Recent Transactions" action={<a href="/admin/transactions" className="text-link" style={{margin:0, fontSize: '0.85rem'}}>View All</a>}>
                    <table className="recent-table">
                        <thead>
                            <tr>
                                <th>Resident</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>A-101 (Malhar Malap)</td>
                                <td>₹5,000</td>
                                <td><span className="chip-success">Paid</span></td>
                                <td>22 Jul 2026</td>
                            </tr>
                            <tr>
                                <td>B-205 (Sarah Jones)</td>
                                <td>₹4,500</td>
                                <td><span className="chip-warning">Pending</span></td>
                                <td>21 Jul 2026</td>
                            </tr>
                            <tr>
                                <td>C-302 (Ravi Kumar)</td>
                                <td>₹5,000</td>
                                <td><span className="chip-success">Paid</span></td>
                                <td>20 Jul 2026</td>
                            </tr>
                        </tbody>
                    </table>
                </Card>
                
                <Card className="glass" title="Latest Announcements" action={<a href="/admin/announcements" className="text-link" style={{margin:0, fontSize: '0.85rem'}}>View All</a>}>
                    <div className="recent-announcements">
                        <div className="ann-card">
                            <div className="ann-icon"><Bell size={16}/></div>
                            <div className="ann-content">
                                <h4>The Ganesh Chaturthi Celebration</h4>
                                <p>Society will be celebrating Ganesh Chaturthi on 7th September 2026.</p>
                            </div>
                            <div className="ann-date">
                                <strong>20</strong>
                                <span>JUL</span>
                            </div>
                        </div>
                        <div className="ann-card">
                            <div className="ann-icon"><Bell size={16}/></div>
                            <div className="ann-content">
                                <h4>Water Supply Interruption</h4>
                                <p>Water supply will be paused from 12 PM to 4 PM.</p>
                            </div>
                            <div className="ann-date">
                                <strong>18</strong>
                                <span>JUL</span>
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
};

export default AdminDashboard;
