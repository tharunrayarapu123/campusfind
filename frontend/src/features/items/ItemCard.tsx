import React from 'react';
import { Link } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import {
  MapPin,
  Clock,
  Laptop,
  Smartphone,
  BookOpen,
  Briefcase,
  CreditCard,
  Watch,
  Coffee,
  Package,
} from 'lucide-react';
import type { ItemCategory, ItemSummary } from '../../types';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';

interface ItemCardProps {
  item: ItemSummary;
}

export const getCategoryIcon = (category: ItemCategory) => {
  switch (category) {
    case 'WATCH':
      return <Watch className="w-5 h-5 text-amber-500" />;
    case 'ID_CARD':
      return <CreditCard className="w-5 h-5 text-indigo-500" />;
    case 'BOTTLE':
      return <Coffee className="w-5 h-5 text-cyan-500" />;
    case 'BAG':
      return <Briefcase className="w-5 h-5 text-emerald-500" />;
    case 'PHONE':
      return <Smartphone className="w-5 h-5 text-blue-500" />;
    case 'BOOKS':
      return <BookOpen className="w-5 h-5 text-violet-500" />;
    case 'ELECTRONICS':
      return <Laptop className="w-5 h-5 text-purple-500" />;
    default:
      return <Package className="w-5 h-5 text-slate-500" />;
  }
};

export const ItemCard: React.FC<ItemCardProps> = ({ item }) => {
  const firstName = item.owner?.fullName ? item.owner.fullName.split(' ')[0] : 'Student';

  let relativeTime: string;
  try {
    relativeTime = formatDistanceToNow(new Date(item.eventTime), { addSuffix: true });
  } catch {
    relativeTime = 'recently';
  }

  const locationText = [item.blockName, item.roomName].filter(Boolean).join(' • ');

  return (
    <Link to={`/items/${item.id}`} className="block group">
      <Card hoverable className="p-0 overflow-hidden flex flex-col h-full border-slate-200/90 dark:border-slate-800">
        {/* Image / Thumbnail placeholder */}
        <div className="relative aspect-[4/3] w-full bg-slate-100 dark:bg-slate-800/80 overflow-hidden flex items-center justify-center">
          {item.imageUrl ? (
            <img
              src={item.imageUrl}
              alt={item.title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
              loading="lazy"
            />
          ) : (
            <div className="flex flex-col items-center justify-center text-slate-400 p-4">
              <div className="w-14 h-14 rounded-2xl bg-white dark:bg-slate-800 shadow-sm flex items-center justify-center mb-2">
                {getCategoryIcon(item.category)}
              </div>
              <span className="text-xs font-medium uppercase tracking-wider text-slate-400">
                {item.category.replace('_', ' ')}
              </span>
            </div>
          )}

          {/* Badges Overlay */}
          <div className="absolute top-3 left-3 flex flex-wrap gap-1.5 z-10">
            <Badge type={item.type} />
            <Badge status={item.status} />
          </div>

          {item.handedToOffice && (
            <div className="absolute bottom-2.5 left-2.5 z-10">
              <span className="text-[10px] font-semibold bg-slate-900/85 backdrop-blur-sm text-white px-2 py-0.5 rounded-md">
                Office drop-point
              </span>
            </div>
          )}
        </div>

        {/* Content details */}
        <div className="p-4 flex-1 flex flex-col justify-between">
          <div>
            <h3 className="font-semibold text-base text-slate-900 dark:text-white line-clamp-1 group-hover:text-brand-600 dark:group-hover:text-brand-400 transition-colors">
              {item.title}
            </h3>

            {item.description && (
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
                {item.description}
              </p>
            )}
          </div>

          <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800/80 flex flex-col gap-1.5 text-xs text-slate-500 dark:text-slate-400">
            {locationText && (
              <div className="flex items-center gap-1.5 line-clamp-1">
                <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                <span className="truncate">{locationText}</span>
              </div>
            )}

            <div className="flex items-center justify-between pt-0.5">
              <div className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                <span>{relativeTime}</span>
              </div>

              <div className="font-medium text-slate-700 dark:text-slate-300">
                by {firstName}
              </div>
            </div>
          </div>
        </div>
      </Card>
    </Link>
  );
};
