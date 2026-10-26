import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, CalendarClock, CheckCircle2, FileText, Megaphone, MessageSquarePlus, RefreshCw, UserRound } from 'lucide-react';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import StatusChip from '../components/ui/StatusChip';
import { Alert, EmptyState, Skeleton } from '../components/ui/Feedback';
import PaymentModal from '../components/PaymentModal';
import { useAuth } from '../context/AuthContext';
import { getResidentDashboard } from '../api/residentApi';
import { getMyTransactions } from '../api/billingApi';
import { firstName, formatDate, formatMoney, greeting, monthLabel } from '../utils/format';

const UNPAID = ['PENDING', 'PARTIALLY_PAID'];

// What is still owed on a bill: its balance (amount + late fee - already paid), never just the face amount
const remaining = (bill) => Number(bill.balance ?? (Number(bill.amount || 0) + Number(bill.lateFee || 0) - Number(bill.paidAmount || 0)));

const isOverdue = (bill) => bill.dueDate && new Date(bill.dueDate) < new Date(new Date().toDateString());

const ResidentDashboard = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
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
            setError('We could not load your home screen. Check your connection and try again.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    if (isLoading) {
        return (
            <div aria-busy="true" aria-label="Loading">
                <Skeleton width={260} height={34} style={{ marginBottom: 28 }} />
                <div className="res-grid">
                    <Skeleton height={230} style={{ borderRadius: 14 }} />
                    <Skeleton height={230} style={{ borderRadius: 14 }} />
                </div>
            </div>
        );
    }

    if (error || !dashboardData) {
        return (
            <Alert tone="error" title="Something went wrong" action={<Button variant="secondary" size="sm" icon={RefreshCw} onClick={load}>Try again</Button>}>
                {error || 'Your dashboard is unavailable right now.'}
            </Alert>
        );
    }

    const { outstandingAmount, unpaidBills, lastPayment, announcements, nextDueDate } = dashboardData;
    const hasDues = outstandingAmount > 0;
    const overdue = unpaidBills.some(isOverdue);

    return (
        <div className="stack stack-lg">
            <header className="page-header" style={{ marginBottom: 0 }}>
                <div className="grow">
                    <h1>{greeting()}, {firstName(user?.name) || 'neighbour'}</h1>
                    <p className="page-description">Your home at Jaswanti Residency, at a glance.</p>
                </div>
            </header>

            <div className="res-grid">
                <section className={`balance-card ${hasDues ? '' : 'is-clear'}`} aria-label="Amount due">
                    <div className="balance-top">
                        <span className="balance-label">{hasDues ? 'Amount due' : 'Your account'}</span>
                        {hasDues && <StatusChip tone={overdue ? 'danger' : 'warning'} label={overdue ? 'Overdue' : 'Due soon'} />}
                    </div>
                    {hasDues ? (
                        <>
                            <div className="balance-amount num">{formatMoney(outstandingAmount, 2)}</div>
                            <p className="balance-meta">
                                <CalendarClock size={15} aria-hidden="true" />
                                {unpaidBills.length} unpaid bill{unpaidBills.length === 1 ? '' : 's'}{nextDueDate ? ` · next due ${formatDate(nextDueDate)}` : ''}
                            </p>
                            <div className="balance-actions">
                                <Button size="lg" onClick={() => setIsPaymentModalOpen(true)}>Pay now</Button>
                                <Button size="lg" variant="ghost" onClick={() => navigate('/resident/invoices')}>See bills</Button>
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="balance-clear"><CheckCircle2 size={26} aria-hidden="true" /> All paid up</div>
                            <p className="balance-meta">Thank you — there is nothing to pay right now.</p>
                            <div className="balance-actions">
                                <Button variant="secondary" onClick={() => navigate('/resident/invoices')}>View payment history</Button>
                            </div>
                        </>
                    )}
                </section>

                <Card title="Last payment" action={<Link to="/resident/invoices" className="card-link">History <ArrowRight size={14} /></Link>}>
                    {lastPayment ? (
                        <div className="stack stack-sm">
                            <div className="last-pay-amount num">{formatMoney(lastPayment.amount, 2)}</div>
                            <div className="text-muted text-sm">Paid on {formatDate(lastPayment.paymentDate || lastPayment.timestamp)}</div>
                            {lastPayment.receiptNumber && <div className="tag" style={{ alignSelf: 'flex-start' }}>Receipt {lastPayment.receiptNumber}</div>}
                        </div>
                    ) : (
                        <EmptyState compact icon={FileText}>No payments yet. Receipts appear here after your first payment.</EmptyState>
                    )}
                </Card>
            </div>

            {hasDues && (
                <Card title="Unpaid bills" flush action={<Link to="/resident/invoices" className="card-link">All bills <ArrowRight size={14} /></Link>}>
                    <ul className="bill-list">
                        {unpaidBills.slice(0, 4).map((b) => (
                            <li key={b.id}>
                                <div className="grow">
                                    <div className="fw-600">{monthLabel(b.billingMonth) || 'Maintenance'}</div>
                                    <div className="cell-sub">{(b.chargeType || 'Maintenance').replace(/_/g, ' ').toLowerCase()} · due {formatDate(b.dueDate)}</div>
                                </div>
                                {isOverdue(b) ? <StatusChip tone="danger" label="Overdue" /> : <StatusChip status={b.status} />}
                                <span className="num fw-600 bill-amt">{formatMoney(remaining(b), 2)}</span>
                            </li>
                        ))}
                    </ul>
                </Card>
            )}

            <div className="quick-links">
                <button type="button" className="quick-link" onClick={() => navigate('/resident/complaints')}>
                    <span className="stat-icon tone-brand"><MessageSquarePlus size={18} /></span>
                    <span><strong>Raise a request</strong><span>Plumbing, electrical, housekeeping or a question</span></span>
                    <ArrowRight size={16} className="text-subtle" />
                </button>
                <button type="button" className="quick-link" onClick={() => navigate('/resident/invoices')}>
                    <span className="stat-icon tone-success"><FileText size={18} /></span>
                    <span><strong>Download receipts</strong><span>Every payment, ready as a PDF</span></span>
                    <ArrowRight size={16} className="text-subtle" />
                </button>
                <button type="button" className="quick-link" onClick={() => navigate('/resident/profile')}>
                    <span className="stat-icon tone-info"><UserRound size={18} /></span>
                    <span><strong>My profile</strong><span>Contact details, flat and password</span></span>
                    <ArrowRight size={16} className="text-subtle" />
                </button>
            </div>

            <Card title="Notice board">
                {announcements.length === 0 ? (
                    <EmptyState compact icon={Megaphone} title="No notices right now">New notices from the committee will show up here.</EmptyState>
                ) : (
                    <div className="notice-grid">
                        {announcements.map((a) => (
                            <article key={a.id} className="notice">
                                <div className="notice-date">{formatDate(a.createdDate, { day: 'numeric', month: 'short' })}</div>
                                <h4>{a.title}</h4>
                                {a.description && <p>{a.description}</p>}
                            </article>
                        ))}
                    </div>
                )}
            </Card>

            {isPaymentModalOpen && (
                <PaymentModal
                    amount={Number(outstandingAmount.toFixed(2))}
                    bills={unpaidBills}
                    onClose={() => { setIsPaymentModalOpen(false); load(); }}
                />
            )}
        </div>
    );
};

export default ResidentDashboard;
