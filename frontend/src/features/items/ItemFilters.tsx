import React from 'react';
import { Filter, X } from 'lucide-react';
import type { Block, CategoryInfo, ItemCategory, ItemStatus } from '../../types';
import { Select } from '../../components/ui/Select';
import { Button } from '../../components/ui/Button';

export interface FilterState {
  category?: ItemCategory;
  blockId?: number;
  roomId?: number;
  status?: ItemStatus;
}

interface ItemFiltersProps {
  filters: FilterState;
  onChange: (filters: FilterState) => void;
  blocks: Block[];
  categories: CategoryInfo[];
  onClear: () => void;
  className?: string;
}

const STATUS_OPTIONS = [
  { value: 'OPEN', label: 'Open' },
  { value: 'MATCHED', label: 'Matched' },
  { value: 'RETURNED', label: 'Returned' },
  { value: 'CLOSED', label: 'Closed' },
];

export const ItemFilters: React.FC<ItemFiltersProps> = ({
  filters,
  onChange,
  blocks,
  categories,
  onClear,
  className,
}) => {
  const selectedBlock = blocks.find((b) => b.id === filters.blockId);
  const rooms = selectedBlock?.rooms || [];

  const hasActiveFilters =
    !!filters.category || !!filters.blockId || !!filters.roomId || !!filters.status;

  const handleBlockChange = (blockIdStr: string) => {
    const blockId = blockIdStr ? Number(blockIdStr) : undefined;
    onChange({
      ...filters,
      blockId,
      roomId: undefined, // Reset room when block changes
    });
  };

  return (
    <div className={className}>
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-200 dark:border-slate-800">
        <div className="flex items-center gap-2 font-semibold text-sm text-slate-800 dark:text-slate-200">
          <Filter className="w-4 h-4 text-brand-600" />
          <span>Filter Items</span>
        </div>
        {hasActiveFilters && (
          <button
            onClick={onClear}
            className="text-xs text-brand-600 dark:text-brand-400 font-medium hover:underline flex items-center gap-1"
          >
            <X className="w-3.5 h-3.5" />
            Clear all
          </button>
        )}
      </div>

      <div className="space-y-4">
        {/* Category */}
        <Select
          label="Category"
          placeholder="All Categories"
          value={filters.category || ''}
          onChange={(e) =>
            onChange({
              ...filters,
              category: (e.target.value as ItemCategory) || undefined,
            })
          }
          options={categories.map((c) => ({
            value: c.name,
            label: c.displayName,
          }))}
        />

        {/* Campus Block */}
        <Select
          label="Campus Block"
          placeholder="All Blocks"
          value={filters.blockId?.toString() || ''}
          onChange={(e) => handleBlockChange(e.target.value)}
          options={blocks.map((b) => ({
            value: b.id,
            label: b.name,
          }))}
        />

        {/* Room (Dependent on Block) */}
        <Select
          label="Classroom / Hall"
          placeholder={filters.blockId ? 'All Rooms' : 'Select block first'}
          disabled={!filters.blockId}
          value={filters.roomId?.toString() || ''}
          onChange={(e) =>
            onChange({
              ...filters,
              roomId: e.target.value ? Number(e.target.value) : undefined,
            })
          }
          options={rooms.map((r) => ({
            value: r.id,
            label: r.name,
          }))}
        />

        {/* Status */}
        <Select
          label="Status"
          placeholder="All Statuses"
          value={filters.status || ''}
          onChange={(e) =>
            onChange({
              ...filters,
              status: (e.target.value as ItemStatus) || undefined,
            })
          }
          options={STATUS_OPTIONS}
        />

        {hasActiveFilters && (
          <div className="pt-2">
            <Button variant="outline" size="sm" onClick={onClear} className="w-full">
              Reset Filters
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
