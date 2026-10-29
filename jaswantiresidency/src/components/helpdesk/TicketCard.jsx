import React from 'react';
import { CalendarRange, MessageCircleReply, Paperclip } from 'lucide-react';
import StatusChip from '../ui/StatusChip';
import { viewAuthenticatedFile } from '../../api/fileUtils';
import { formatDate, timeAgo } from '../../utils/format';

export const TYPE_LABELS = { COMPLAINT: 'Complaint', QUERY: 'Question', SUGGESTION: 'Suggestion', PERMISSION: 'Permission' };
const TYPE_TONES = { COMPLAINT: 'danger', QUERY: 'info', SUGGESTION: 'warning', PERMISSION: 'success' };

/** One helpdesk request: what was asked, by whom, and the committee's reply. Shared by both portals. */
const TicketCard = ({ ticket, showResident = false, actions, onOpen }) => {
    const type = ticket.submissionType || 'COMPLAINT';
    const interactive = Boolean(onOpen);
    return (
        <article
            className={`ticket ${interactive ? 'is-link' : ''}`}
            onClick={onOpen}
            onKeyDown={interactive ? (e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onOpen(); } } : undefined}
            tabIndex={interactive ? 0 : undefined}
        >
            <header className="ticket-head">
                <span className={`ticket-type tone-${TYPE_TONES[type] || 'neutral'}`}>{TYPE_LABELS[type] || type}</span>
                {ticket.category && <span className="ticket-cat">{ticket.category.replace(/_/g, ' ').toLowerCase()}</span>}
                <span className="ticket-meta">#{ticket.id} · {timeAgo(ticket.createdDate)}</span>
                <StatusChip status={ticket.status} className="ticket-status" />
            </header>
            {showResident && (
                <div className="ticket-who">{ticket.roomNumber || '—'} · {ticket.residentName || 'Resident'}</div>
            )}
            <p className="ticket-body">{ticket.description}</p>
            {(type === 'PERMISSION' || ticket.hasAttachment) && (
                <div className="ticket-extras">
                    {type === 'PERMISSION' && (
                        <span><CalendarRange size={14} /> {(ticket.permissionCategory || '').replace(/_/g, ' ').toLowerCase() || 'permission'} · {formatDate(ticket.startDate)} – {formatDate(ticket.endDate)}</span>
                    )}
                    {ticket.hasAttachment && (
                        <button type="button" className="btn btn-link btn-sm" onClick={(e) => { e.stopPropagation(); viewAuthenticatedFile(`/complaints/${ticket.id}/attachment`, `request_${ticket.id}_attachment`); }}>
                            <Paperclip size={14} /> View attachment
                        </button>
                    )}
                </div>
            )}
            {ticket.adminResponse && (
                <div className="ticket-reply">
                    <MessageCircleReply size={15} aria-hidden="true" />
                    <div><span className="ticket-reply-label">Committee reply</span>{ticket.adminResponse}</div>
                </div>
            )}
            {actions && <footer className="ticket-actions" onClick={(e) => e.stopPropagation()}>{actions}</footer>}
        </article>
    );
};

export default TicketCard;
