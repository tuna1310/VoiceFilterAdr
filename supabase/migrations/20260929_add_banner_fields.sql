-- Add banner/game-mode/skin support for Home game-preview cards.
-- Priority in app: word banner > category banner > gradient fallback.

alter table if exists public.words
  add column if not exists banner text,
  add column if not exists game_mode text,
  add column if not exists skin text;

alter table if exists public.categories
  add column if not exists banner text,
  add column if not exists default_game_mode text,
  add column if not exists skin text;

comment on column public.words.banner is
  'Optional WebP banner URL/path for a word, e.g. file:///android_asset/banners/banner_colors_brown.webp or CDN URL.';

comment on column public.words.game_mode is
  'Game mode key requested by this word/card, e.g. voice_runner or category_rush.';

comment on column public.words.skin is
  'Visual skin/filter key for rendering game previews and challenge theme.';

comment on column public.categories.banner is
  'Optional category fallback WebP banner URL/path used when a word has no banner.';

comment on column public.categories.default_game_mode is
  'Default game mode key used when a category is launched without a specific word.';

comment on column public.categories.skin is
  'Category-level visual skin/filter key for rendering previews and challenge theme.';
