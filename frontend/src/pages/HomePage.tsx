import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Search,
  SlidersHorizontal,
  Plus,
  RefreshCw,
  X,
} from 'lucide-react';
import { itemsApi } from '../api/items';
import { referenceApi } from '../api/reference';
import type { ItemType, ItemSummary } from '../types';
import { ItemCard } from '../features/items/ItemCard';
import { ItemFilters, type FilterState } from '../features/items/ItemFilters';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';

export const HomePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'ALL' | 'LOST' | 'FOUND'>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [debouncedKeyword, setDebouncedKeyword] = useState<string>('');
  const [filters, setFilters] = useState<FilterState>({});
  const [page, setPage] = useState<number>(0);
  const [items, setItems] = useState<ItemSummary[]>([]);
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState<boolean>(false);

  // Debounce search query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedKeyword(searchQuery.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Fetch reference blocks & categories
  const { data: blocks = [] } = useQuery({
    queryKey: ['referenceBlocks'],
    queryFn: referenceApi.getBlocks,
    staleTime: 1000 * 60 * 10,
  });

  const { data: categories = [] } = useQuery({
    queryKey: ['referenceCategories'],
    queryFn: referenceApi.getCategories,
    staleTime: 1000 * 60 * 10,
  });

  // Query items feed
  const itemTypeParam: ItemType | undefined = activeTab === 'ALL' ? undefined : activeTab;

  const {
    data: itemsPage,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['itemsFeed', itemTypeParam, debouncedKeyword, filters, page],
    queryFn: () =>
      itemsApi.getItems({
        type: itemTypeParam,
        keyword: debouncedKeyword || undefined,
        category: filters.category,
        blockId: filters.blockId,
        roomId: filters.roomId,
        status: filters.status,
        page,
        size: 12,
      }),
  });

  // Accumulate items for pagination
  useEffect(() => {
    if (itemsPage?.content) {
      if (page === 0) {
        setItems(itemsPage.content);
      } else {
        setItems((prev) => [...prev, ...itemsPage.content]);
      }
    }
  }, [itemsPage, page]);

  const handleTabChange = (tab: 'ALL' | 'LOST' | 'FOUND') => {
    setActiveTab(tab);
    setPage(0);
  };

  const handleFilterChange = (newFilters: FilterState) => {
    setFilters(newFilters);
    setPage(0);
  };

  const handleClearFilters = () => {
    setFilters({});
    setPage(0);
  };

  const hasNextPage = itemsPage ? !itemsPage.last : false;
  const activeFilterCount = Object.values(filters).filter(Boolean).length;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4 sm:pt-6">
      {/* Top Header & Search Bar */}
      <div className="flex flex-col gap-4 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
              Campus Lost & Found
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
              Discover and match lost belongings across campus facilities
            </p>
          </div>

          <Link to="/post" className="hidden sm:inline-block">
            <Button variant="primary" className="shadow-sm">
              <Plus className="w-4 h-4 mr-1.5" /> Post Item
            </Button>
          </Link>
        </div>

        {/* Search Input & Mobile Filter Button */}
        <div className="flex items-center gap-2.5">
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search items by title or description..."
              className="w-full min-h-touch rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 pl-10 pr-10 py-2.5 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <button
            onClick={() => setIsMobileFilterOpen(true)}
            className="lg:hidden flex items-center justify-center gap-1.5 min-h-touch px-3.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
          >
            <SlidersHorizontal className="w-4 h-4 text-brand-600" />
            <span className="hidden sm:inline">Filters</span>
            {activeFilterCount > 0 && (
              <span className="w-5 h-5 rounded-full bg-brand-600 text-white text-[11px] font-bold flex items-center justify-center">
                {activeFilterCount}
              </span>
            )}
          </button>
        </div>

        {/* Tabs: ALL, LOST, FOUND */}
        <div className="flex border-b border-slate-200 dark:border-slate-800 gap-6 text-sm font-medium">
          {(['ALL', 'LOST', 'FOUND'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => handleTabChange(tab)}
              className={`pb-3 relative transition-colors ${
                activeTab === tab
                  ? 'text-brand-600 dark:text-brand-400 font-semibold'
                  : 'text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'
              }`}
            >
              {tab === 'ALL' ? 'All Items' : tab === 'LOST' ? 'Lost Items' : 'Found Items'}
              {activeTab === tab && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-brand-600 dark:bg-brand-400 rounded-full" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Main Grid & Desktop Sidebar Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Desktop Sidebar Filters */}
        <aside className="hidden lg:block lg:col-span-1">
          <div className="sticky top-20 bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200/90 dark:border-slate-800 shadow-soft">
            <ItemFilters
              filters={filters}
              onChange={handleFilterChange}
              blocks={blocks}
              categories={categories}
              onClear={handleClearFilters}
            />
          </div>
        </aside>

        {/* Items Grid Content */}
        <div className="lg:col-span-3">
          {/* Error State */}
          {isError && (
            <div className="p-6 rounded-2xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-center my-6">
              <h3 className="text-base font-semibold text-rose-800 dark:text-rose-200 mb-1">
                Unable to load items
              </h3>
              <p className="text-sm text-rose-600 dark:text-rose-400 mb-4">
                {error instanceof Error ? error.message : 'Please check your connection.'}
              </p>
              <Button variant="outline" size="sm" onClick={() => refetch()}>
                <RefreshCw className="w-4 h-4 mr-1.5" /> Try Again
              </Button>
            </div>
          )}

          {/* Loading Skeletons */}
          {isLoading && page === 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
              {Array.from({ length: 6 }).map((_, i) => (
                <div
                  key={i}
                  className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-0 overflow-hidden"
                >
                  <Skeleton className="aspect-[4/3] w-full" />
                  <div className="p-4 space-y-3">
                    <Skeleton className="h-5 w-3/4 rounded-md" />
                    <Skeleton className="h-4 w-full rounded-md" />
                    <div className="pt-2 flex justify-between">
                      <Skeleton className="h-4 w-1/3 rounded-md" />
                      <Skeleton className="h-4 w-1/4 rounded-md" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!isLoading && !isError && items.length === 0 && (
            <EmptyState
              title="No items found"
              description={
                debouncedKeyword || activeFilterCount > 0
                  ? 'No items matched your current search filters. Try adjusting keywords or clearing filters.'
                  : 'No items posted yet in this category. Be the first to report a lost or found item.'
              }
              actionText="Post an Item"
              onAction={() => (window.location.href = '/post')}
            />
          )}

          {/* Items Grid */}
          {!isLoading && items.length > 0 && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
                {items.map((item) => (
                  <ItemCard key={item.id} item={item} />
                ))}
              </div>

              {/* Load More Button */}
              {hasNextPage && (
                <div className="mt-8 text-center">
                  <Button
                    variant="secondary"
                    isLoading={isFetching && page > 0}
                    onClick={() => setPage((prev) => prev + 1)}
                    className="min-w-[160px]"
                  >
                    Load More Items
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Floating Action Button (Mobile Only) */}
      <Link
        to="/post"
        aria-label="Post an item"
        className="sm:hidden fixed bottom-20 right-5 z-30 w-14 h-14 rounded-full bg-brand-600 text-white shadow-elevated flex items-center justify-center hover:bg-brand-700 active:scale-95 transition-all"
      >
        <Plus className="w-7 h-7" />
      </Link>

      {/* Mobile Filters Bottom Sheet */}
      {isMobileFilterOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex flex-col justify-end">
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setIsMobileFilterOpen(false)}
          />
          <div className="relative bg-white dark:bg-slate-900 rounded-t-3xl p-6 border-t border-slate-200 dark:border-slate-800 max-h-[85vh] overflow-y-auto shadow-2xl z-10 animate-in slide-in-from-bottom duration-200">
            <ItemFilters
              filters={filters}
              onChange={handleFilterChange}
              blocks={blocks}
              categories={categories}
              onClear={handleClearFilters}
            />
            <div className="mt-6 pt-4 border-t border-slate-100 dark:border-slate-800">
              <Button
                variant="primary"
                className="w-full"
                onClick={() => setIsMobileFilterOpen(false)}
              >
                Apply Filters
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
