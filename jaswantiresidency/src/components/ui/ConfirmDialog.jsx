import React from 'react';
import { AlertTriangle } from 'lucide-react';
import Modal from './Modal';
import Button from './Button';

const ConfirmDialog = ({
    isOpen, title, message, onConfirm, onCancel, confirmText = 'Confirm', cancelText = 'Cancel',
    confirmVariant = 'danger', isLoading = false, icon = AlertTriangle, tone,
}) => (
    <Modal
        isOpen={isOpen}
        onClose={onCancel}
        title={title}
        icon={icon}
        tone={tone || (confirmVariant === 'danger' ? 'danger' : 'brand')}
        size="sm"
        dismissible={!isLoading}
        footer={(
            <>
                <Button variant="secondary" onClick={onCancel} disabled={isLoading}>{cancelText}</Button>
                <Button variant={confirmVariant} onClick={onConfirm} isLoading={isLoading} data-autofocus>{confirmText}</Button>
            </>
        )}
    >
        {typeof message === 'string' ? <p className="text-muted">{message}</p> : message}
    </Modal>
);

export default ConfirmDialog;
