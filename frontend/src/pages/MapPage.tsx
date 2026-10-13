import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import {
  MapPin,
  List,
  Map as MapIcon,
  Package,
} from 'lucide-react';
import { itemsApi } from '../api/items';
import type { ItemSummary, ItemType } from '../types';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { cn } from '../lib/utils';

// Center of campus (configurable campus coordinates)
const CAMPUS_CENTER: [number, number] = [17.4455, 78.3489];

// Fix Leaflet custom pin icons using SVG DivIcon
const createCustomPin = (type: ItemType) => {
  const isLost = type === 'LOST';
  const color = isLost ? '#f43f5e' : '#10b981';
  const label = isLost ? 'L' : 'F';

  return L.divIcon({
    className: 'custom-leaflet-pin',
    html: `
      <div style="
        background-color: ${color};
        width: 30px;
        height: 30px;
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.2);
        border: 2px solid white;
      ">
        <span style="
          transform: rotate(45deg);
          color: white;
          font-weight: 800;
          font-size: 11px;
          font-family: sans-serif;
        ">${label}</span>
      </div>
    `,
    iconSize: [30, 30],
    iconAnchor: [15, 30],
    popupAnchor: [0, -32],
  });
};

export const MapPage: React.FC = () => {
  const [viewMode, setViewMode] = useState<'MAP' | 'LIST'>('MAP');
  const [selectedType, setSelectedType] = useState<'ALL' | ItemType>('ALL');

  const { data: pageData, isLoading } = useQuery({
    queryKey: ['mapItems'],
    queryFn: () => itemsApi.getItems({ size: 100 }),
  });

  const allItems: ItemSummary[] = pageData?.content || [];

  // Filter items
  const filteredItems = allItems.filter((item) => {
    if (selectedType !== 'ALL' && item.type !== selectedType) return false;
    return true;
  });

  // Assign deterministic mock coords around campus for items without lat/long for demonstration
  const getCoordinates = (index: number): [number, number] => {
    const angle = (index * 47) % 360;
    const radius = 0.002 + ((index % 5) * 0.001);
    const latOffset = Math.sin((angle * Math.PI) / 180) * radius;
    const lngOffset = Math.cos((angle * Math.PI) / 180) * radius;
    return [CAMPUS_CENTER[0] + latOffset, CAMPUS_CENTER[1] + lngOffset];
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-4">
      {/* Top Filter Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-xs">
        <div>
          <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2">
            <MapPin className="w-6 h-6 text-brand-600" />
            Campus Lost & Found Map
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Geographic view of recent lost and found reports across campus blocks.
          </p>
        </div>

        {/* Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Type Filter */}
          <div className="flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1">
            {(['ALL', 'LOST', 'FOUND'] as const).map((t) => (
              <button
                key={t}
                onClick={() => setSelectedType(t)}
                className={cn(
                  'px-3 py-1 rounded-lg text-xs font-bold transition-all',
                  selectedType === t
                    ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                    : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
                )}
              >
                {t}
              </button>
            ))}
          </div>

          {/* View Toggle */}
          <div className="flex rounded-xl border border-slate-200 dark:border-slate-800 p-1">
            <button
              onClick={() => setViewMode('MAP')}
              className={cn(
                'p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-colors',
                viewMode === 'MAP'
                  ? 'bg-brand-600 text-white'
                  : 'text-slate-500 hover:text-slate-800'
              )}
            >
              <MapIcon className="w-4 h-4" />
              <span className="hidden sm:inline">Map</span>
            </button>
            <button
              onClick={() => setViewMode('LIST')}
              className={cn(
                'p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition-colors',
                viewMode === 'LIST'
                  ? 'bg-brand-600 text-white'
                  : 'text-slate-500 hover:text-slate-800'
              )}
            >
              <List className="w-4 h-4" />
              <span className="hidden sm:inline">List</span>
            </button>
          </div>
        </div>
      </div>

      {/* Main Map or Fallback List */}
      {viewMode === 'MAP' ? (
        <div className="h-[650px] w-full rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 shadow-sm relative z-0">
          <MapContainer
            center={CAMPUS_CENTER}
            zoom={16}
            scrollWheelZoom={true}
            style={{ height: '100%', width: '100%' }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {filteredItems.map((item, idx) => {
              const coords = getCoordinates(idx);
              return (
                <Marker
                  key={item.id}
                  position={coords}
                  icon={createCustomPin(item.type)}
                >
                  <Popup className="custom-leaflet-popup">
                    <div className="p-1 space-y-2 max-w-[220px]">
                      {item.imageUrl && (
                        <div className="h-24 w-full rounded-lg overflow-hidden bg-slate-100">
                          <img
                            src={item.imageUrl}
                            alt={item.title}
                            className="w-full h-full object-cover"
                          />
                        </div>
                      )}
                      <div>
                        <div className="flex items-center gap-1.5 mb-1">
                          <span
                            className={cn(
                              'px-1.5 py-0.2 rounded text-[9px] font-bold uppercase',
                              item.type === 'LOST'
                                ? 'bg-rose-100 text-rose-700'
                                : 'bg-emerald-100 text-emerald-700'
                            )}
                          >
                            {item.type}
                          </span>
                          <span className="text-[10px] text-slate-500 font-medium">
                            {item.category}
                          </span>
                        </div>
                        <h4 className="text-xs font-bold text-slate-900 leading-snug line-clamp-2">
                          {item.title}
                        </h4>
                        <p className="text-[10px] text-slate-500 mt-0.5">
                          {item.blockName || 'Campus'} • {new Date(item.eventTime).toLocaleDateString()}
                        </p>
                      </div>

                      <Link
                        to={`/items/${item.id}`}
                        className="inline-flex items-center gap-1 text-[11px] font-bold text-brand-600 hover:underline pt-1"
                      >
                        View Post Details →
                      </Link>
                    </div>
                  </Popup>
                </Marker>
              );
            })}
          </MapContainer>
        </div>
      ) : (
        /* Fallback List View */
        <div className="space-y-3">
          {isLoading ? (
            <Skeleton className="h-32 w-full rounded-2xl" />
          ) : filteredItems.length === 0 ? (
            <EmptyState
              icon={<Package className="w-8 h-8 text-slate-400" />}
              title="No items found"
              description="No reports match your current filters."
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredItems.map((item) => (
                <Card key={item.id} className="p-4 flex gap-3">
                  <div className="w-16 h-16 rounded-xl bg-slate-100 dark:bg-slate-800 overflow-hidden shrink-0 flex items-center justify-center">
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt={item.title}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <Package className="w-6 h-6 text-slate-400" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <span
                      className={cn(
                        'px-1.5 py-0.2 rounded-md text-[10px] font-bold uppercase',
                        item.type === 'LOST'
                          ? 'bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300'
                          : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                      )}
                    >
                      {item.type}
                    </span>
                    <h4 className="text-sm font-bold text-slate-900 dark:text-white truncate mt-1">
                      {item.title}
                    </h4>
                    <span className="text-xs text-slate-500 block truncate">
                      {item.blockName || 'Campus'} • {new Date(item.eventTime).toLocaleDateString()}
                    </span>
                    <Link
                      to={`/items/${item.id}`}
                      className="text-xs font-semibold text-brand-600 dark:text-brand-400 hover:underline mt-2 inline-block"
                    >
                      View Details →
                    </Link>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MapPage;
