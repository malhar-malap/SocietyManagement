import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card from '../components/ui/Card';
import { Plus, Paperclip, MessageSquare } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useAuth } from '../context/AuthContext';
import { submitComplaint } from '../api/adminApi';

const ResidentComplaints = () => {
    const { user } = useAuth();
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [selectedTab, setSelectedTab] = useState('ALL');
    const [submissionType, setSubmissionType] = useState('COMPLAINT');
    const { register, handleSubmit, reset, formState: { errors } } = useForm();

    const [complaints, setComplaints] = useState([]);

    const fetchComplaints = async () => {
        setIsLoading(true);
        try {
            const { getComplaints } = await import('../api/complaintApi');
            const data = await getComplaints();
            const sorted = (data || []).sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
            setComplaints(sorted);
        } catch (err) {
            console.error("Failed to fetch requests", err);
            setComplaints([]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchComplaints();
    }, [user]);

    const filteredComplaints = complaints.filter(c => {
        if (selectedTab === 'ALL') return true;
        return c.submissionType === selectedTab;
    });

    const getTypeBadge = (type) => {
        const styles = {
            COMPLAINT: { bg: '#fee2e2', text: '#991b1b' },
            QUERY: { bg: '#e0e7ff', text: '#3730a3' },
            SUGGESTION: { bg: '#fef3c7', text: '#92400e' },
            PERMISSION: { bg: '#dcfce7', text: '#166534' }
        };
        const s = styles[type] || styles.COMPLAINT;
        return (
            <span style={{ background: s.bg, color: s.text, padding: '0.15rem 0.5rem', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700 }}>
                {type || 'COMPLAINT'}
            </span>
        );
    };

    const columns = [
        { header: 'ID', accessor: 'id' },
        {
            header: 'Type',
            render: (row) => getTypeBadge(row.submissionType)
        },
        { header: 'Category', accessor: 'category' },
        {
            header: 'Subject & Description',
            render: (row) => (
                <div style={{ maxWidth: '300px' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>{row.title}</div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary-light)' }}>{row.description}</div>
                    {row.submissionType === 'PERMISSION' && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--primary)', marginTop: '0.2rem' }}>
                            Dates: {row.startDate || '?'} to {row.endDate || '?'} ({row.permissionCategory})
                        </div>
                    )}
                    {row.hasAttachment && (
                        <a
                            href={`/api/complaints/${row.id}/attachment`}
                            target="_blank"
                            rel="noreferrer"
                            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: '#2563eb', marginTop: '0.2rem' }}
                        >
                            <Paperclip size={12} /> View Attached Document
                        </a>
                    )}
                </div>
            )
        },
        {
            header: 'Admin Feedback',
            render: (row) => row.adminResponse ? (
                <div style={{ background: '#f8fafc', padding: '0.4rem 0.6rem', borderRadius: 6, fontSize: '0.8rem', borderLeft: '3px solid var(--primary)' }}>
                    <strong>Response:</strong> {row.adminResponse}
                </div>
            ) : (
                <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Awaiting society response</span>
            )
        },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> }
    ];

    const onSubmit = async (data) => {
        setIsSubmitting(true);
        try {
            const formData = new FormData();
            formData.append('submissionType', submissionType);
            formData.append('category', data.category || 'General');
            formData.append('description', (data.title ? data.title + " - " : "") + data.description);

            if (submissionType === 'PERMISSION') {
                formData.append('permissionCategory', data.permissionCategory);
                if (data.startDate) formData.append('startDate', data.startDate);
                if (data.endDate) formData.append('endDate', data.endDate);
            }

            if (data.file && data.file.length > 0) {
                formData.append('file', data.file[0]);
            }

            await submitComplaint(formData);
            await fetchComplaints();
            setIsFormVisible(false);
            reset();
        } catch (err) {
            console.error("Failed to submit request", err);
            alert(err?.response?.data?.message || "Failed to submit request. Please try again.");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div>
            <PageHeader
                title="Helpdesk & Requests"
                breadcrumbs={['Resident', 'Helpdesk']}
                actionButton={
                    !isFormVisible && (
                        <Button onClick={() => setIsFormVisible(true)}>
                            <Plus size={18} /> New Request
                        </Button>
                    )
                }
            />

            {/* Filter Tabs */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
                {['ALL', 'COMPLAINT', 'QUERY', 'SUGGESTION', 'PERMISSION'].map(type => (
                    <button
                        key={type}
                        onClick={() => setSelectedTab(type)}
                        style={{
                            padding: '0.45rem 0.85rem',
                            borderRadius: '6px',
                            border: selectedTab === type ? '1px solid var(--primary)' : '1px solid var(--border-light)',
                            background: selectedTab === type ? 'var(--primary)' : '#fff',
                            color: selectedTab === type ? '#fff' : 'var(--text-secondary-light)',
                            fontWeight: 600,
                            fontSize: '0.85rem',
                            cursor: 'pointer'
                        }}
                    >
                        {type === 'ALL' ? 'All My Requests' : type.charAt(0) + type.slice(1).toLowerCase() + 's'}
                    </button>
                ))}
            </div>

            {isFormVisible && (
                <Card title="Submit New Request / Helpdesk Ticket" className="mb-8" style={{ marginBottom: '1.5rem' }}>
                    <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div>
                            <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>
                                Request Type <span style={{ color: 'red' }}>*</span>
                            </label>
                            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                                {[
                                    { value: 'COMPLAINT', label: 'Complaint' },
                                    { value: 'QUERY', label: 'Query / Question' },
                                    { value: 'SUGGESTION', label: 'Suggestion' },
                                    { value: 'PERMISSION', label: 'Permission Request' }
                                ].map(opt => (
                                    <label key={opt.value} style={{
                                        display: 'flex', alignItems: 'center', gap: '0.4rem',
                                        padding: '0.4rem 0.8rem', borderRadius: 6,
                                        border: submissionType === opt.value ? '2px solid var(--primary)' : '1px solid var(--border-light)',
                                        background: submissionType === opt.value ? '#f0f7ff' : '#fff',
                                        cursor: 'pointer', fontSize: '0.85rem', fontWeight: 600
                                    }}>
                                        <input
                                            type="radio"
                                            name="subTypeRadio"
                                            value={opt.value}
                                            checked={submissionType === opt.value}
                                            onChange={() => setSubmissionType(opt.value)}
                                        />
                                        {opt.label}
                                    </label>
                                ))}
                            </div>
                        </div>

                        {submissionType === 'PERMISSION' && (
                            <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: 8, border: '1px solid var(--border-light)', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                                <div>
                                    <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>
                                        Permission Category <span style={{ color: 'red' }}>*</span>
                                    </label>
                                    <select
                                        className="custom-input"
                                        style={{ width: '100%', padding: '0.5rem' }}
                                        {...register('permissionCategory', { required: submissionType === 'PERMISSION' })}
                                    >
                                        <option value="RENOVATION">Flat Renovation / Civil Work</option>
                                        <option value="INTERIOR_WORK">Carpentry & Painting Work</option>
                                        <option value="MOVE_IN_OUT">Move-in / Move-out Loading</option>
                                        <option value="ADDRESS_PROOF">Address Proof / NOC Letter</option>
                                        <option value="OTHER">Other Society Permission</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>
                                        Start Date <span style={{ color: 'red' }}>*</span>
                                    </label>
                                    <input
                                        type="date"
                                        className="custom-input"
                                        style={{ width: '100%', padding: '0.5rem' }}
                                        {...register('startDate', { required: submissionType === 'PERMISSION' })}
                                    />
                                </div>
                                <div>
                                    <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>
                                        End Date <span style={{ color: 'red' }}>*</span>
                                    </label>
                                    <input
                                        type="date"
                                        className="custom-input"
                                        style={{ width: '100%', padding: '0.5rem' }}
                                        {...register('endDate', { required: submissionType === 'PERMISSION' })}
                                    />
                                </div>
                            </div>
                        )}

                        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                            <div style={{ flex: '1 1 200px' }}>
                                <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>Category</label>
                                <select
                                    className="custom-input"
                                    style={{ width: '100%', padding: '0.5rem' }}
                                    {...register('category')}
                                >
                                    <option value="Maintenance">General Maintenance</option>
                                    <option value="Plumbing">Plumbing</option>
                                    <option value="Electrical">Electrical</option>
                                    <option value="Security">Security & Parking</option>
                                    <option value="Accounts">Accounts & Billing</option>
                                    <option value="Other">Other</option>
                                </select>
                            </div>
                            <div style={{ flex: '2 1 300px' }}>
                                <Input
                                    label="Subject / Title"
                                    placeholder="Brief summary of request"
                                    error={errors.title?.message}
                                    {...register('title', { required: 'Title is required' })}
                                />
                            </div>
                        </div>

                        <div>
                            <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>Detailed Description</label>
                            <textarea
                                className="custom-input"
                                style={{ width: '100%', padding: '0.5rem' }}
                                rows="3"
                                placeholder="Provide complete details..."
                                {...register('description', { required: 'Description is required' })}
                            ></textarea>
                            {errors.description && <span className="error-message">{errors.description.message}</span>}
                        </div>

                        <div>
                            <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>Attach Photo / Supporting Document (Optional)</label>
                            <input
                                type="file"
                                accept=".pdf,image/*"
                                style={{ fontSize: '0.85rem' }}
                                {...register('file')}
                            />
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                            <Button type="button" variant="secondary" onClick={() => setIsFormVisible(false)} disabled={isSubmitting}>
                                Cancel
                            </Button>
                            <Button type="submit" isLoading={isSubmitting}>
                                Submit {submissionType.charAt(0) + submissionType.slice(1).toLowerCase()}
                            </Button>
                        </div>
                    </form>
                </Card>
            )}

            <DataTable
                columns={columns}
                data={filteredComplaints}
                isLoading={isLoading}
                emptyMessage="No helpdesk requests found."
            />
        </div>
    );
};

export default ResidentComplaints;
