import React, { useEffect, useMemo, useState } from 'react';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import './ui.css';

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
 * Client-side table with search and pagination. Search used to only call an onSearch callback that no page
 * passed, so typing did nothing; it now filters rows itself (onSearch is still called if provided).
 */
const DataTable = ({ columns, data, onSearch, emptyMessage = 'No data available', isLoading = false, searchPlaceholder = 'Search...' }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(1);

    const filtered = useMemo(() => {
        const rows = data || [];
        const term = searchTerm.trim().toLowerCase();
        if (!term || onSearch) return rows;
        return rows.filter((row) => searchableText(row, columns).includes(term));
    }, [data, searchTerm, columns, onSearch]);

    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));

    // Go back to page 1 when the search or the data changes, so a shorter list never shows an empty page
    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, data]);

    const page = Math.min(currentPage, totalPages);
    const paginatedData = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
    const firstRow = filtered.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
    const lastRow = Math.min(page * PAGE_SIZE, filtered.length);

    const handleSearch = (e) => {
        setSearchTerm(e.target.value);
        if (onSearch) onSearch(e.target.value);
    };

    return (
        <div className="data-table-container">
            <div className="table-toolbar">
                <div className="search-wrapper">
                    <Search size={18} className="search-icon" aria-hidden="true" />
                    <input
                        type="search"
                        placeholder={searchPlaceholder}
                        aria-label="Search table"
                        value={searchTerm}
                        onChange={handleSearch}
                        className="table-search"
                    />
                </div>
            </div>

            <div className="table-responsive">
                <table className="custom-table">
                    <thead>
                        <tr>
                            {columns.map((col, idx) => (
                                <th key={idx}>{col.header}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr>
                                <td colSpan={columns.length} className="text-center py-8">
                                    <div className="spinner mx-auto"></div>
                                </td>
                            </tr>
                        ) : paginatedData.length > 0 ? (
                            paginatedData.map((row, idx) => (
                                <tr key={row.id ?? idx}>
                                    {columns.map((col, colIdx) => (
                                        <td key={colIdx}>
                                            {col.render ? col.render(row) : row[col.accessor]}
                                        </td>
                                    ))}
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={columns.length} className="text-center py-8 empty-state">
                                    {searchTerm.trim() && (data || []).length > 0 ? `No results for "${searchTerm.trim()}"` : emptyMessage}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {!isLoading && filtered.length > 0 && (
                <div className="pagination">
                    <span className="page-info">Showing {firstRow}–{lastRow} of {filtered.length}</span>
                    <div className="page-controls">
                        <button
                            type="button"
                            className="page-btn"
                            aria-label="Previous page"
                            disabled={page === 1}
                            onClick={() => setCurrentPage(page - 1)}
                        >
                            <ChevronLeft size={16} />
                        </button>
                        <span className="page-info">Page {page} of {totalPages}</span>
                        <button
                            type="button"
                            className="page-btn"
                            aria-label="Next page"
                            disabled={page === totalPages}
                            onClick={() => setCurrentPage(page + 1)}
                        >
                            <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default DataTable;
