import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronLeft, ChevronRight, Download, RotateCcw, ScrollText, Search } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import StatusChip from '../components/ui/StatusChip';
import { Select, TextInput } from '../components/ui/Field';
import { Alert, EmptyState, Skeleton } from '../components/ui/Feedback';
import { initials } from '../layouts/PortalLayout';
import { exportSystemLogs, getSystemLogFilters, getSystemLogs } from '../api/adminApi';

const EMPTY_FILTERS = { q: '', module: '', outcome: '', actorRole: '', from: '', to: '' };
const PAGE_SIZES = [25, 50, 100];
const ROLES = ['MASTER_ADMIN', 'ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT'];

const OUTCOME_TONE = { SUCCESS: 'success', FAILED: 'warning', DENIED: 'danger' };

// APPROVE_PAYMENT_SUBMISSION -> "Approve payment submission"
const humanize = (value) => {
    if (!value) return '';
    const text = value.toLowerCase().replace(/_/g, ' ');
    return text.charAt(0).toUpperCase() + text.slice(1);
};

const formatTimestamp = (iso) => (iso ? new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit', second: '2-digit' }) : '');

const relativeTime = (iso) => {
    if (!iso) return '';
    const seconds = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours} h ago`;
    const days = Math.round(hours / 24);
    return days < 30 ? `${days} d ago` : formatTimestamp(iso);
};

// Drops empty values so the API only receives filters that are actually set
const toParams = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ''));

const SystemLogs = () => {
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [searchText, setSearchText] = useState('');
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(25);
    const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0 });
    const [options, setOptions] = useState({ modules: [], actions: [], outcomes: ['SUCCESS', 'FAILED', 'DENIED'] });
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [isExporting, setIsExporting] = useState(false);
    const [expandedId, setExpandedId] = useState(null);

    // Debounce free-text search so typing doesn't fire a request per keystroke
    useEffect(() => {
        const timer = setTimeout(() => {
            setFilters((f) => (f.q === searchText.trim() ? f : { ...f, q: searchText.trim() }));
            setPage(0);
        }, 350);
        return () => clearTimeout(timer);
    }, [searchText]);

    useEffect(() => {
        getSystemLogFilters().then(setOptions).catch(() => { /* filters still work as free text */ });
    }, []);

    const load = useCallback(async () => {
        setIsLoading(true);
        setError('');
        try {
            const data = await getSystemLogs({ ...toParams(filters), page, size });
            setResult(data);
        } catch (err) {
            const status = err?.response?.status;
            setError(status === 403
                ? 'Only the master administrator can view the system logs.'
                : 'Could not load the system logs. Check your connection and try again.');
            setResult({ content: [], totalElements: 0, totalPages: 0 });
        } finally {
            setIsLoading(false);
        }
    }, [filters, page, size]);

    useEffect(() => { load(); }, [load]);

    const updateFilter = (key, value) => {
        setFilters((f) => ({ ...f, [key]: value }));
        setPage(0);
    };

    const resetFilters = () => {
        setSearchText('');
        setFilters(EMPTY_FILTERS);
        setPage(0);
    };

    const handleExport = async () => {
        setIsExporting(true);
        try {
            await exportSystemLogs(toParams(filters));
        } catch {
            setError('Export failed. Please try again.');
        } finally {
            setIsExporting(false);
        }
    };

    const hasFilters = useMemo(() => searchText !== '' || Object.values(filters).some((v) => v !== ''), [filters, searchText]);
    const firstRow = result.totalElements === 0 ? 0 : page * size + 1;
    const lastRow = Math.min((page + 1) * size, result.totalElements);

    return (
        <div>
            <PageHeader
                title="System logs"
                description="Every important action, including failed and refused attempts. Entries are permanent and cannot be edited or deleted."
                actions={(
                    <Button variant="secondary" icon={Download} onClick={handleExport} isLoading={isExporting} disabled={result.totalElements === 0}>
                        Export CSV
                    </Button>
                )}
            />

            <div className="card filter-bar" role="search">
                <div className="table-search filter-q">
                    <Search size={16} aria-hidden="true" />
                    <input type="search" aria-label="Search logs" placeholder="Search person, flat, receipt, title…" value={searchText} onChange={(e) => setSearchText(e.target.value)} />
                </div>
                <Select aria-label="Module" value={filters.module} onChange={(e) => updateFilter('module', e.target.value)}>
                    <option value="">All modules</option>
                    {options.modules.map((m) => <option key={m} value={m}>{humanize(m)}</option>)}
                </Select>
                <Select aria-label="Outcome" value={filters.outcome} onChange={(e) => updateFilter('outcome', e.target.value)}>
                    <option value="">All outcomes</option>
                    {options.outcomes.map((o) => <option key={o} value={o}>{humanize(o)}</option>)}
                </Select>
                <Select aria-label="Role" value={filters.actorRole} onChange={(e) => updateFilter('actorRole', e.target.value)}>
                    <option value="">All roles</option>
                    {ROLES.map((r) => <option key={r} value={r}>{humanize(r)}</option>)}
                </Select>
                <TextInput aria-label="From date" type="date" value={filters.from} max={filters.to || undefined} onChange={(e) => updateFilter('from', e.target.value)} />
                <TextInput aria-label="To date" type="date" value={filters.to} min={filters.from || undefined} onChange={(e) => updateFilter('to', e.target.value)} />
                {hasFilters && <Button variant="ghost" icon={RotateCcw} onClick={resetFilters}>Reset</Button>}
            </div>

            {error && (
                <Alert tone="error" className="mb-4" action={<Button variant="secondary" size="sm" onClick={load}>Try again</Button>}>{error}</Alert>
            )}

            <div className="table-card">
                <div className="table-scroll">
                    <table className="table log-table">
                        <thead>
                            <tr>
                                <th>When</th>
                                <th>Who</th>
                                <th>What happened</th>
                                <th>Outcome</th>
                                <th><span className="visually-hidden">Details</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {isLoading ? (
                                Array.from({ length: 8 }).map((_, i) => (
                                    <tr key={`skeleton-${i}`} aria-hidden="true">
                                        <td><Skeleton width={70} /></td><td><Skeleton width="70%" /></td><td><Skeleton width="85%" /></td><td><Skeleton width={60} /></td><td />
                                    </tr>
                                ))
                            ) : result.content.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="empty">
                                        <EmptyState compact icon={ScrollText} title={hasFilters ? 'No matching entries' : 'Nothing recorded yet'}>
                                            {hasFilters ? 'Try widening the dates or clearing a filter.' : 'Actions will be recorded here as people use the portal.'}
                                        </EmptyState>
                                    </td>
                                </tr>
                            ) : (
                                result.content.map((log) => {
                                    const expanded = expandedId === log.id;
                                    const who = log.actorName || log.actorEmail || 'Unknown';
                                    return (
                                        <React.Fragment key={log.id}>
                                            <tr className={`log-row outcome-${log.outcome?.toLowerCase()} ${expanded ? 'is-open' : ''}`} onClick={() => setExpandedId(expanded ? null : log.id)}>
                                                <td className="nowrap text-muted" title={formatTimestamp(log.timestamp)}>{relativeTime(log.timestamp)}</td>
                                                <td>
                                                    <div className="row" style={{ '--gap': '0.6rem' }}>
                                                        <span className="avatar avatar-sm" aria-hidden="true">{initials(who.includes('@') ? who.split('@')[0] : who)}</span>
                                                        <div style={{ minWidth: 0 }}>
                                                            <div className="cell-primary log-actor">{who}</div>
                                                            {log.actorRole && <div className="cell-sub">{humanize(log.actorRole)}</div>}
                                                        </div>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div className="row row-wrap" style={{ '--gap': '0.45rem' }}>
                                                        <span className="fw-600">{humanize(log.action)}</span>
                                                        <span className="tag">{humanize(log.module)}</span>
                                                    </div>
                                                    <div className="cell-sub log-description">{log.description}</div>
                                                    {log.targetLabel && <div className="log-target">{log.targetLabel}</div>}
                                                </td>
                                                <td><StatusChip tone={OUTCOME_TONE[log.outcome] || 'neutral'} label={humanize(log.outcome)} /></td>
                                                <td className="right">
                                                    <button type="button" className="icon-btn icon-btn-sm" aria-expanded={expanded} aria-label={expanded ? 'Hide details' : 'Show details'}
                                                        onClick={(e) => { e.stopPropagation(); setExpandedId(expanded ? null : log.id); }}>
                                                        <ChevronDown size={16} style={{ transform: expanded ? 'rotate(180deg)' : 'none', transition: 'transform 160ms' }} />
                                                    </button>
                                                </td>
                                            </tr>
                                            {expanded && (
                                                <tr className="log-details-row">
                                                    <td colSpan={5}>
                                                        <dl className="detail-list">
                                                            <div><dt>Exact time</dt><dd>{formatTimestamp(log.timestamp)}</dd></div>
                                                            <div><dt>Person</dt><dd>{log.actorName || '—'}{log.actorEmail ? ` · ${log.actorEmail}` : ''}</dd></div>
                                                            <div><dt>Action code</dt><dd><code>{log.action}</code></dd></div>
                                                            <div><dt>Target</dt><dd>{log.targetType ? `${humanize(log.targetType)} #${log.targetId}` : '—'}</dd></div>
                                                            <div><dt>Request</dt><dd><code>{log.httpMethod} {log.requestPath}</code></dd></div>
                                                            <div><dt>IP address</dt><dd>{log.ipAddress || '—'}</dd></div>
                                                            <div style={{ gridColumn: '1 / -1' }}><dt>Description</dt><dd>{log.description}</dd></div>
                                                        </dl>
                                                    </td>
                                                </tr>
                                            )}
                                        </React.Fragment>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="table-footer">
                    <span className="num">{result.totalElements > 0 ? `Showing ${firstRow}–${lastRow} of ${result.totalElements}` : 'No entries'}</span>
                    <div className="pager">
                        <label className="row text-sm" style={{ '--gap': '0.4rem' }}>
                            Rows
                            <select className="pager-select" value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}>
                                {PAGE_SIZES.map((sz) => <option key={sz} value={sz}>{sz}</option>)}
                            </select>
                        </label>
                        <button type="button" className="pager-btn" aria-label="Previous page" disabled={page === 0 || isLoading} onClick={() => setPage((p) => p - 1)}>
                            <ChevronLeft size={16} />
                        </button>
                        <span className="num">Page {result.totalPages === 0 ? 0 : page + 1} of {result.totalPages}</span>
                        <button type="button" className="pager-btn" aria-label="Next page" disabled={page + 1 >= result.totalPages || isLoading} onClick={() => setPage((p) => p + 1)}>
                            <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SystemLogs;
