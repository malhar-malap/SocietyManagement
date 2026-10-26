import React from 'react';
import { Trash2 } from 'lucide-react';
import ConfirmDialog from './ui/ConfirmDialog';

const DeleteConfirmationDialog = ({ isOpen, onClose, onConfirm, title, itemName, isDeleting }) => (
    <ConfirmDialog
        isOpen={isOpen}
        title={title}
        icon={Trash2}
        message={<p className="text-muted">Delete <strong className="text-brand" style={{ color: 'var(--text)' }}>{itemName}</strong>? This cannot be undone.</p>}
        confirmText="Delete"
        confirmVariant="danger"
        onConfirm={onConfirm}
        onCancel={onClose}
        isLoading={isDeleting}
    />
);

export default DeleteConfirmationDialog;
