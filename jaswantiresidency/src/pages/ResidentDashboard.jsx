import React, { useState, useEffect } from 'react';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import { IndianRupee, CheckCircle2, AlertCircle, Clock } from 'lucide-react';
import PaymentModal from '../components/PaymentModal';
import { useAuth } from '../context/AuthContext';
import './dashboard.css';

const ResidentDashboard = () => {
    const { user } = useAuth();
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);

    const [dashboardData, setDashboardData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchDashboard = async () => {
            try {
                const { getResidentDashboard } = await import('../api/residentApi');
                const data = await getResidentDashboard();

                const unpaidBills = (data.bills || []).filter(b => b.status === 'PENDING');
                const outstandingAmount = unpaidBills.reduce((acc, bill) => acc + bill.amount, 0);

                const paidBills = (data.bills || []).filter(b => b.status !== 'PENDING');
                const lastPayment = paidBills.length > 0 ? paidBills[paidBills.length - 1] : null;

                const nextDueDate = unpaidBills.length > 0 ? unpaidBills[0].dueDate : 'N/A';

                setDashboardData({
                    outstandingAmount,
                    dueDate: nextDueDate,
                    lastPayment: lastPayment ? { amount: lastPayment.amount, date: 'Recently' } : null,
                    announcements: (data.announcements || []).map(a => ({
                        id: a.id,
                        title: a.title,
                        date: a.createdDate ? new Date(a.createdDate).toLocaleDateString() : 'N/A',
                        type: 'info'
                    })),
                    unpaidBills
                });
            } catch (err) {
                // Fallback to stub if backend isn't ready
                setDashboardData({
                    outstandingAmount: 3500,
                    dueDate: '2026-08-01',
                    lastPayment: { amount: 2500, date: '2026-07-05' },
                    announcements: [
                        { id: 1, title: 'Scheduled Power Outage', date: 'Tomorrow, 10 AM - 2 PM', type: 'warning' },
                        { id: 2, title: 'Society Meeting', date: 'Sunday, 11 AM', type: 'info' }
                    ]
                });
            } finally {
                setIsLoading(false);
            }
        };
        fetchDashboard();
    }, []);

    if (isLoading) return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading dashboard...</div>;

    return (
        <div className="dashboard-wrapper animate-fade-up">

            {/* Hero Section */}
            <div className="dashboard-hero">
                <h1 className="hero-greeting">Good morning, {user?.name} 👋</h1>
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
                        <h1 className="amount-large">₹{dashboardData.outstandingAmount}</h1>
                        <p className="due-date text-warning">Due by {dashboardData.dueDate}</p>
                        <Button
                            onClick={() => setIsPaymentModalOpen(true)}
                            style={{width: '100%', marginTop: '1.5rem'}}
                        >
                            Pay Now
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
                                <h2 className="amount-medium">₹{dashboardData.lastPayment.amount}</h2>
                                <p className="subtitle">Paid {dashboardData.lastPayment.date}</p>
                            </>
                        ) : (
                            <p className="subtitle" style={{marginTop: '1rem'}}>No previous payments found.</p>
                        )}
                        <a href="/resident/invoices" className="text-link" style={{marginTop: '1rem', display: 'inline-block'}}>View Invoice History</a>
                    </div>
                </Card>

                {/* Announcements Widget */}
                <Card className="widget-card col-span-2 glass animate-scale-in" style={{animationDelay: '0.3s'}}>
                    <div className="widget-header">
                        <h3 className="widget-title">Recent Announcements</h3>
                        <AlertCircle className="widget-icon text-primary" />
                    </div>
                    <div className="announcement-list">
                        {dashboardData.announcements.map(ann => (
                            <div key={ann.id} className="announcement-item">
                                <div className="ann-content">
                                    <h4 className="ann-title">{ann.title}</h4>
                                    <span className="ann-date"><Clock size={14}/> {ann.date}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </Card>
            </div>

            {/* Render Payment Modal */}
            {isPaymentModalOpen && (
                <PaymentModal
                    amount={dashboardData.outstandingAmount}
                    bills={dashboardData.unpaidBills}
                    onClose={() => setIsPaymentModalOpen(false)}
                />
            )}
        </div>
    );
};

export default ResidentDashboard;
