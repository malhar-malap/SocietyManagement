import React, { useEffect, useMemo, useState } from 'react';
import { Search, ChevronLeft, ChevronRight, Inbox } from 'lucide-react';
import { EmptyState, Skeleton } from './Feedback';

const PAGE_SIZE = 10;

// Text a row can be found by: every column's accessor value plus any other primitive field on the row
const searchableText = (row, columns) => {
    const parts = [];
    columns.forEach((col) => {
        if (col.accessor && row[col.accessor] != null) parts.push(row[col.accessor]);
        if (col.searchValue) parts.push(col.searchValue(row));
    });
    Object.values(row || {}).forEach((v) => {
        if (v != null && (typeof v === 'string' || typeof v === 'number')) parts.push(v);
    });
    return parts.join(' ').toLowerCase();
};

/**
 * Client-side table with search and pagination.
 * Columns: { header, accessor | render(row), align: 'right', width, searchValue(row) }.
 * `toolbar` renders next to the search box (filters, export buttons); `emptyAction` under the empty message.
 */
const DataTable = ({
    columns, data, onSearch, emptyMessage = 'Nothing here yet', emptyTitle, emptyIcon = Inbox, emptyAction,
    isLoading = false, searchPlaceholder = 'Search…', toolbar, onRowClick, rowClassName, pageSize = PAGE_SIZE, searchable = true,
}) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(1);

    const filtered = useMemo(() => {
        const rows = data || [];
        const term = searchTerm.trim().toLowerCase();
        if (!term || onSearch) return rows;
        return rows.filter((row) => searchableText(row, columns).includes(term));
    }, [data, searchTerm, columns, onSearch]);

    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));

    // Go back to page 1 when the search or the data changes, so a shorter list never shows an empty page
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, data]);

    const page = Math.min(currentPage, totalPages);
    const paginatedData = filtered.slice((page - 1) * pageSize, page * pageSize);
    const firstRow = filtered.length === 0 ? 0 : (page - 1) * pageSize + 1;
    const lastRow = Math.min(page * pageSize, filtered.length);

    const handleSearch = (e) => {
        setSearchTerm(e.target.value);
        if (onSearch) onSearch(e.target.value);
    };

    const noResults = searchTerm.trim() && (data || []).length > 0;

    return (
        <div className="table-card">
            {(searchable || toolbar) && (
                <div className="table-toolbar">
                    {searchable && (
                        <div className="table-search">
                            <Search size={16} aria-hidden="true" />
                            <input
                                type="search"
                                placeholder={searchPlaceholder}
                                aria-label="Search table"
                                value={searchTerm}
                                onChange={handleSearch}
                            />
                        </div>
                    )}
                    {toolbar && <div className="row row-wrap">{toolbar}</div>}
                </div>
            )}

            <div className="table-scroll">
                <table className="table table-stack">
                    <thead>
                        <tr>
                            {columns.map((col, idx) => (
                                <th key={idx} className={col.align === 'right' ? 'right' : undefined} style={col.width ? { width: col.width } : undefined}>
                                    {col.header}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            Array.from({ length: 5 }).map((_, r) => (
                                <tr key={`sk-${r}`}>
                                    {columns.map((col, c) => (
                                        <td key={c}><Skeleton width={c === 0 ? '70%' : '50%'} /></td>
                                    ))}
                                </tr>
                            ))
                        ) : paginatedData.length > 0 ? (
                            paginatedData.map((row, idx) => (
                                <tr
                                    key={row.id ?? idx}
                                    className={rowClassName ? rowClassName(row) : undefined}
                                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                                    style={onRowClick ? { cursor: 'pointer' } : undefined}
                                >
                                    {columns.map((col, colIdx) => (
                                        <td
                                            key={colIdx}
                                            className={col.align === 'right' ? 'right num' : undefined}
                                            data-label={typeof col.header === 'string' ? col.header : undefined}
                                        >
                                            {col.render ? col.render(row) : row[col.accessor]}
                                        </td>
                                    ))}
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={columns.length} className="empty">
                                    <EmptyState icon={noResults ? Search : emptyIcon} title={noResults ? undefined : emptyTitle} compact action={noResults ? undefined : emptyAction}>
                                        {noResults ? `No results for "${searchTerm.trim()}"` : emptyMessage}
                                    </EmptyState>
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {!isLoading && filtered.length > 0 && (
                <div className="table-footer">
                    <span className="num">Showing {firstRow}–{lastRow} of {filtered.length}</span>
                    {totalPages > 1 && (
                        <div className="pager">
                            <button type="button" className="pager-btn" aria-label="Previous page" disabled={page === 1} onClick={() => setCurrentPage(page - 1)}>
                                <ChevronLeft size={16} />
                            </button>
                            <span className="num">Page {page} of {totalPages}</span>
                            <button type="button" className="pager-btn" aria-label="Next page" disabled={page === totalPages} onClick={() => setCurrentPage(page + 1)}>
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default DataTable;
