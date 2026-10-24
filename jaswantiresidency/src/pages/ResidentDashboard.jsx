import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import { IndianRupee, CheckCircle2, AlertCircle, AlertTriangle, Clock } from 'lucide-react';
import PaymentModal from '../components/PaymentModal';
import { useAuth } from '../context/AuthContext';
import { getResidentDashboard } from '../api/residentApi';
import { getMyTransactions } from '../api/billingApi';
import './dashboard.css';

const UNPAID = ['PENDING', 'PARTIALLY_PAID'];

const formatMoney = (value) =>
    `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const formatDate = (value) => (value ? new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '');

// What is still owed on a bill: its balance (amount + late fee - already paid), never just the face amount
const remaining = (bill) => Number(bill.balance ?? (Number(bill.amount || 0) + Number(bill.lateFee || 0) - Number(bill.paidAmount || 0)));

const ResidentDashboard = () => {
    const { user } = useAuth();
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
    const [dashboardData, setDashboardData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setIsLoading(true);
        setError('');
        try {
            const [dashboard, transactions] = await Promise.allSettled([getResidentDashboard(), getMyTransactions()]);
            if (dashboard.status !== 'fulfilled') throw dashboard.reason;
            const data = dashboard.value || {};

            const unpaidBills = (data.bills || [])
                .filter((b) => UNPAID.includes(b.status) && remaining(b) > 0)
                .sort((a, b) => String(a.dueDate || '').localeCompare(String(b.dueDate || '')));
            const outstandingAmount = unpaidBills.reduce((sum, b) => sum + remaining(b), 0);

            const payments = transactions.status === 'fulfilled'
                ? (transactions.value || [])
                    .filter((t) => t.status === 'SUCCESS' && t.entryDirection !== 'DEBIT')
                    .sort((a, b) => new Date(b.paymentDate || b.timestamp || 0) - new Date(a.paymentDate || a.timestamp || 0))
                : [];

            setDashboardData({
                outstandingAmount,
                nextDueDate: unpaidBills.length > 0 ? unpaidBills[0].dueDate : null,
                lastPayment: payments[0] || null,
                announcements: data.announcements || [],
                unpaidBills,
            });
        } catch (err) {
            // Never show placeholder amounts to a resident: say it failed and offer a retry
            setError('Could not load your dashboard. Check your connection and try again.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    if (isLoading) {
        return (
            <div className="dashboard-wrapper" aria-busy="true">
                <div className="skeleton" style={{ height: 32, width: 260, marginBottom: '1.5rem' }} />
                <div className="dashboard-grid">
                    <div className="skeleton" style={{ height: 180 }} />
                    <div className="skeleton" style={{ height: 180 }} />
                </div>
            </div>
        );
    }

    if (error || !dashboardData) {
        return (
            <div className="dashboard-wrapper">
                <Card>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--danger)' }}>
                        <AlertTriangle size={20} aria-hidden="true" />
                        <span style={{ flex: 1 }}>{error || 'Dashboard data is unavailable.'}</span>
                        <Button variant="secondary" onClick={load}>Retry</Button>
                    </div>
                </Card>
            </div>
        );
    }

    const hasDues = dashboardData.outstandingAmount > 0;

    return (
        <div className="dashboard-wrapper animate-fade-up">

            {/* Hero Section */}
            <div className="dashboard-hero">
                <h1 className="hero-greeting">Hello, {user?.name || 'Resident'} 👋</h1>
                <p className="hero-subtext">Welcome back to Jaswanti Residency.</p>
            </div>

            <div className="dashboard-grid mt-4">

                {/* Outstanding Balance Widget */}
                <Card className="widget-card highlight-card animate-scale-in" style={{animationDelay: '0.1s'}}>
                    <div className="widget-header">
                        <h3 className="widget-title">Outstanding Balance</h3>
                        <IndianRupee className="widget-icon highlight-icon" />
                    </div>
                    <div className="widget-body">
                        <h1 className="amount-large">{formatMoney(dashboardData.outstandingAmount)}</h1>
                        {hasDues ? (
                            <p className="due-date text-warning">
                                {dashboardData.unpaidBills.length} unpaid bill{dashboardData.unpaidBills.length === 1 ? '' : 's'}
                                {dashboardData.nextDueDate ? ` · next due ${formatDate(dashboardData.nextDueDate)}` : ''}
                            </p>
                        ) : (
                            <p className="due-date text-success">All dues are cleared. Thank you!</p>
                        )}
                        <Button
                            onClick={() => setIsPaymentModalOpen(true)}
                            disabled={!hasDues}
                            style={{width: '100%', marginTop: '1.5rem'}}
                        >
                            {hasDues ? 'Pay Now' : 'Nothing to pay'}
                        </Button>
                    </div>
                </Card>

                {/* Last Payment Widget */}
                <Card className="widget-card animate-scale-in" style={{animationDelay: '0.2s'}}>
                    <div className="widget-header">
                        <h3 className="widget-title">Last Payment</h3>
                        <CheckCircle2 className="widget-icon text-success" />
                    </div>
                    <div className="widget-body">
                        {dashboardData.lastPayment ? (
                            <>
                                <h2 className="amount-medium">{formatMoney(dashboardData.lastPayment.amount)}</h2>
                                <p className="subtitle">
                                    Paid {formatDate(dashboardData.lastPayment.paymentDate || dashboardData.lastPayment.timestamp)}
                                    {dashboardData.lastPayment.receiptNumber ? ` · Receipt ${dashboardData.lastPayment.receiptNumber}` : ''}
                                </p>
                            </>
                        ) : (
                            <p className="subtitle" style={{marginTop: '1rem'}}>No previous payments found.</p>
                        )}
                        <Link to="/resident/invoices" className="text-link" style={{marginTop: '1rem', display: 'inline-block'}}>View Invoice History</Link>
                    </div>
                </Card>

                {/* Announcements Widget */}
                <Card className="widget-card col-span-2 glass animate-scale-in" style={{animationDelay: '0.3s'}}>
                    <div className="widget-header">
                        <h3 className="widget-title">Recent Announcements</h3>
                        <AlertCircle className="widget-icon text-primary" />
                    </div>
                    <div className="announcement-list">
                        {dashboardData.announcements.length === 0 ? (
                            <p className="subtitle">No announcements right now.</p>
                        ) : dashboardData.announcements.map(ann => (
                            <div key={ann.id} className="announcement-item">
                                <div className="ann-content">
                                    <h4 className="ann-title">{ann.title}</h4>
                                    {ann.description && <p className="subtitle" style={{ margin: '0.25rem 0' }}>{ann.description}</p>}
                                    <span className="ann-date"><Clock size={14}/> {formatDate(ann.createdDate) || 'N/A'}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </Card>
            </div>

            {/* Render Payment Modal */}
            {isPaymentModalOpen && (
                <PaymentModal
                    amount={Number(dashboardData.outstandingAmount.toFixed(2))}
                    bills={dashboardData.unpaidBills}
                    onClose={() => { setIsPaymentModalOpen(false); load(); }}
                />
            )}
        </div>
    );
};

export default ResidentDashboard;
