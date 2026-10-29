import React, { useEffect, useState } from 'react';
import { Check, Copy, KeyRound } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { Alert } from './ui/Feedback';
import { copyText } from '../utils/clipboard';

/**
 * Shows a newly issued temporary password exactly once. It is not stored anywhere in the app afterwards, so the
 * admin must pass it on now; the user is forced to choose their own password at first sign-in.
 */
const TemporaryPasswordDialog = ({ info, onClose }) => {
    const [copied, setCopied] = useState(false);
    useEffect(() => { setCopied(false); }, [info]);

    const copy = async () => {
        setCopied(await copyText(info.password));
    };

    return (
        <Modal
            isOpen={Boolean(info)}
            onClose={onClose}
            size="sm"
            icon={KeyRound}
            tone="success"
            title="Temporary password ready"
            description={info ? `Share it with ${info.name} privately.` : undefined}
            footer={<Button onClick={onClose}>Done</Button>}
        >
            {info && (
                <div className="stack">
                    <div className="secret-box">
                        <code>{info.password}</code>
                        <Button variant={copied ? 'soft' : 'secondary'} size="sm" icon={copied ? Check : Copy} onClick={copy} aria-label="Copy temporary password" data-autofocus>
                            {copied ? 'Copied' : 'Copy'}
                        </Button>
                    </div>
                    <Alert tone="warning">This is shown only once. They will choose their own password the first time they sign in.</Alert>
                </div>
            )}
        </Modal>
    );
};

export default TemporaryPasswordDialog;
