import React, { useState } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card from '../components/ui/Card';
import { Plus } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useAuth } from '../context/AuthContext';

const ResidentComplaints = () => {
    const { user } = useAuth();
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const { register, handleSubmit, reset, formState: { errors } } = useForm();

    const [complaints, setComplaints] = useState([]);

    const fetchComplaints = async () => {
        if (!user?.userId) return;
        try {
            const { getComplaintsByResident } = await import('../api/complaintApi');
            const data = await getComplaintsByResident(user.userId);
            // Sort so newest are first
            const sorted = data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
            setComplaints(sorted);
        } catch (err) {
            console.error("Failed to fetch complaints", err);
        } finally {
            setIsLoading(false);
        }
    };

    React.useEffect(() => {
        fetchComplaints();
    }, [user]);

    const columns = [
        { header: 'ID', accessor: 'id' },
        { header: 'Category', accessor: 'category' },
        { header: 'Description', render: (row) => row.description.substring(0, 50) + '...' },
        { header: 'Date', render: (row) => new Date(row.createdAt).toLocaleDateString() },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> }
    ];

    const onSubmit = async (data) => {
        setIsSubmitting(true);
        try {
            const { createComplaint } = await import('../api/complaintApi');
            const formData = new FormData();
            formData.append('residentId', user.userId);
            formData.append('category', data.category);
            formData.append('description', data.title + " - " + data.description);
            if (data.image && data.image.length > 0) {
                formData.append('file', data.image[0]);
            }

            await createComplaint(formData);
            await fetchComplaints();
            setIsFormVisible(false);
            reset();
        } catch (err) {
            console.error("Failed to submit complaint", err);
            alert("Failed to submit complaint. Please try again.");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div>
            <PageHeader 
                title="My Complaints" 
                breadcrumbs={['Resident', 'Complaints']} 
                actionButton={
                    !isFormVisible && (
                        <Button onClick={() => setIsFormVisible(true)}>
                            <Plus size={18} /> New Complaint
                        </Button>
                    )
                }
            />

            {isFormVisible && (
                <Card title="Submit New Complaint" className="mb-8">
                    <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div style={{ display: 'flex', gap: '1rem' }}>
                            <div style={{ flex: 1 }}>
                                <label className="input-label" style={{display: 'block', marginBottom: '0.3rem'}}>Category</label>
                                <select 
                                    className="custom-input w-full" 
                                    {...register('category', { required: 'Category is required' })}
                                >
                                    <option value="">Select Category...</option>
                                    <option value="Plumbing">Plumbing</option>
                                    <option value="Electrical">Electrical</option>
                                    <option value="Maintenance">General Maintenance</option>
                                    <option value="Security">Security</option>
                                    <option value="Other">Other</option>
                                </select>
                                {errors.category && <span className="error-message">{errors.category.message}</span>}
                            </div>
                            <div style={{ flex: 2 }}>
                                <Input 
                                    label="Complaint Title" 
                                    placeholder="Brief description of issue"
                                    error={errors.title?.message}
                                    {...register('title', { required: 'Title is required' })}
                                />
                            </div>
                        </div>
                        
                        <div>
                            <label className="input-label" style={{display: 'block', marginBottom: '0.3rem'}}>Description</label>
                            <textarea 
                                className="custom-input w-full" 
                                rows="4" 
                                placeholder="Provide more details..."
                                {...register('description', { required: 'Description is required' })}
                            ></textarea>
                            {errors.description && <span className="error-message">{errors.description.message}</span>}
                        </div>
                        
                        <div>
                            <label className="input-label" style={{display: 'block', marginBottom: '0.3rem'}}>Attach Image (Optional)</label>
                            <input 
                                type="file" 
                                className="custom-input w-full" 
                                accept="image/*"
                                {...register('image')}
                            />
                        </div>

                        <div className="flex justify-end gap-4 mt-2">
                            <Button type="button" variant="secondary" onClick={() => setIsFormVisible(false)} disabled={isSubmitting}>Cancel</Button>
                            <Button type="submit" isLoading={isSubmitting}>Submit Complaint</Button>
                        </div>
                    </form>
                </Card>
            )}

            <DataTable 
                columns={columns} 
                data={complaints} 
                isLoading={isLoading}
                emptyMessage="No complaints filed yet."
            />
        </div>
    );
};

export default ResidentComplaints;
