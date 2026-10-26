import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Download, RotateCcw, Search, ShieldAlert } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import { exportSystemLogs, getSystemLogFilters, getSystemLogs } from '../api/adminApi';
import './systemLogs.css';

const EMPTY_FILTERS = { q: '', module: '', outcome: '', actorRole: '', from: '', to: '' };
const PAGE_SIZES = [25, 50, 100];
const ROLES = ['MASTER_ADMIN', 'ADMIN', 'RESIDENT'];

const OUTCOME_CHIP = { SUCCESS: 'chip-success', FAILED: 'chip-warning', DENIED: 'chip-danger' };

// APPROVE_PAYMENT_SUBMISSION -> "Approve payment submission"
const humanize = (value) => {
    if (!value) return '';
    const text = value.toLowerCase().replace(/_/g, ' ');
    return text.charAt(0).toUpperCase() + text.slice(1);
};

const formatTimestamp = (iso) => (iso ? new Date(iso).toLocaleString() : '');

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
                ? 'Only Master Admins can view the system logs.'
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
        <div className="system-logs">
            <PageHeader
                title="System Logs"
                breadcrumbs={['Master Admin', 'System Logs']}
                actionButton={
                    <Button variant="secondary" onClick={handleExport} isLoading={isExporting} disabled={isExporting || result.totalElements === 0}>
                        <Download size={16} /> Export CSV
                    </Button>
                }
            />
            <p className="system-logs-intro">
                Every important action is recorded here, including failed and refused attempts. Entries cannot be edited or deleted.
            </p>

            <div className="system-logs-filters" role="search">
                <div className="filter-field filter-search">
                    <label htmlFor="log-search">Search</label>
                    <div className="filter-search-box">
                        <Search size={16} aria-hidden="true" />
                        <input
                            id="log-search"
                            type="search"
                            placeholder="Person, room, receipt, title…"
                            value={searchText}
                            onChange={(e) => setSearchText(e.target.value)}
                        />
                    </div>
                </div>
                <div className="filter-field">
                    <label htmlFor="log-module">Module</label>
                    <select id="log-module" value={filters.module} onChange={(e) => updateFilter('module', e.target.value)}>
                        <option value="">All modules</option>
                        {options.modules.map((m) => <option key={m} value={m}>{humanize(m)}</option>)}
                    </select>
                </div>
                <div className="filter-field">
                    <label htmlFor="log-outcome">Outcome</label>
                    <select id="log-outcome" value={filters.outcome} onChange={(e) => updateFilter('outcome', e.target.value)}>
                        <option value="">All outcomes</option>
                        {options.outcomes.map((o) => <option key={o} value={o}>{humanize(o)}</option>)}
                    </select>
                </div>
                <div className="filter-field">
                    <label htmlFor="log-role">Role</label>
                    <select id="log-role" value={filters.actorRole} onChange={(e) => updateFilter('actorRole', e.target.value)}>
                        <option value="">All roles</option>
                        {ROLES.map((r) => <option key={r} value={r}>{humanize(r)}</option>)}
                    </select>
                </div>
                <div className="filter-field">
                    <label htmlFor="log-from">From</label>
                    <input id="log-from" type="date" value={filters.from} max={filters.to || undefined}
                           onChange={(e) => updateFilter('from', e.target.value)} />
                </div>
                <div className="filter-field">
                    <label htmlFor="log-to">To</label>
                    <input id="log-to" type="date" value={filters.to} min={filters.from || undefined}
                           onChange={(e) => updateFilter('to', e.target.value)} />
                </div>
                {hasFilters && (
                    <button type="button" className="filter-reset" onClick={resetFilters}>
                        <RotateCcw size={14} aria-hidden="true" /> Reset
                    </button>
                )}
            </div>

            {error && (
                <div className="system-logs-error" role="alert">
                    <ShieldAlert size={18} aria-hidden="true" />
                    <span>{error}</span>
                    <button type="button" onClick={load}>Retry</button>
                </div>
            )}

            <div className="data-table-container system-logs-table">
                <div className="table-responsive">
                    <table className="custom-table">
                        <thead>
                            <tr>
                                <th><span className="visually-hidden">Details</span></th>
                                <th>When</th>
                                <th>Outcome</th>
                                <th>Who</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {isLoading ? (
                                Array.from({ length: 6 }).map((_, i) => (
                                    <tr key={`skeleton-${i}`} aria-hidden="true">
                                        <td colSpan={5}><div className="skeleton system-logs-skeleton" /></td>
                                    </tr>
                                ))
                            ) : result.content.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="system-logs-empty">
                                        {hasFilters ? 'No log entries match these filters.' : 'No actions have been recorded yet.'}
                                    </td>
                                </tr>
                            ) : (
                                result.content.map((log) => {
                                    const expanded = expandedId === log.id;
                                    return (
                                        <React.Fragment key={log.id}>
                                            <tr className={`log-row outcome-${log.outcome?.toLowerCase()}`}
                                                onClick={() => setExpandedId(expanded ? null : log.id)}>
                                                <td className="log-expand-cell">
                                                    <button type="button" className="log-expand" aria-expanded={expanded}
                                                            aria-label={expanded ? 'Hide details' : 'Show details'}
                                                            onClick={(e) => { e.stopPropagation(); setExpandedId(expanded ? null : log.id); }}>
                                                        {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                                    </button>
                                                </td>
                                                <td className="log-when" title={formatTimestamp(log.timestamp)}>
                                                    {relativeTime(log.timestamp)}
                                                </td>
                                                <td>
                                                    <span className={`status-chip ${OUTCOME_CHIP[log.outcome] || 'chip-default'}`}>
                                                        {humanize(log.outcome)}
                                                    </span>
                                                </td>
                                                <td>
                                                    <div className="log-actor">{log.actorName || log.actorEmail || 'Unknown'}</div>
                                                    {log.actorRole && <div className="log-sub">{humanize(log.actorRole)}</div>}
                                                </td>
                                                <td>
                                                    <div className="log-action">
                                                        {humanize(log.action)}
                                                        <span className="log-module">{humanize(log.module)}</span>
                                                    </div>
                                                    <div className="log-sub log-description">{log.description}</div>
                                                    {log.targetLabel && <div className="log-target">{log.targetLabel}</div>}
                                                </td>
                                            </tr>
                                            {expanded && (
                                                <tr className="log-details-row">
                                                    <td colSpan={5}>
                                                        <dl className="log-details">
                                                            <div><dt>Exact time</dt><dd>{formatTimestamp(log.timestamp)}</dd></div>
                                                            <div><dt>Actor</dt><dd>{log.actorName || '—'}{log.actorEmail ? ` <${log.actorEmail}>` : ''}</dd></div>
                                                            <div><dt>Action code</dt><dd><code>{log.action}</code></dd></div>
                                                            <div><dt>Target</dt><dd>{log.targetType ? `${humanize(log.targetType)} #${log.targetId}` : '—'}</dd></div>
                                                            <div><dt>Request</dt><dd><code>{log.httpMethod} {log.requestPath}</code></dd></div>
                                                            <div><dt>IP address</dt><dd>{log.ipAddress || '—'}</dd></div>
                                                            <div className="log-details-wide"><dt>Description</dt><dd>{log.description}</dd></div>
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

                <div className="pagination system-logs-pagination">
                    <span className="page-info">
                        {result.totalElements > 0 ? `Showing ${firstRow}–${lastRow} of ${result.totalElements}` : 'No entries'}
                    </span>
                    <div className="page-controls">
                        <label className="page-size">
                            Rows
                            <select value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}>
                                {PAGE_SIZES.map((s) => <option key={s} value={s}>{s}</option>)}
                            </select>
                        </label>
                        <button type="button" className="page-btn" aria-label="Previous page"
                                disabled={page === 0 || isLoading} onClick={() => setPage((p) => p - 1)}>
                            <ChevronLeft size={16} />
                        </button>
                        <span className="page-info">Page {result.totalPages === 0 ? 0 : page + 1} of {result.totalPages}</span>
                        <button type="button" className="page-btn" aria-label="Next page"
                                disabled={page + 1 >= result.totalPages || isLoading} onClick={() => setPage((p) => p + 1)}>
                            <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SystemLogs;
