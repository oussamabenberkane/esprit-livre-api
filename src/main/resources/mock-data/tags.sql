-- Insert tags data
-- Table: tag
-- Columns: id, name_en, name_fr, type, active, color_hex, image_url

-- CATEGORY tags
INSERT INTO tag (id, name_en, name_fr, type, active, color_hex, image_url) VALUES
(1, 'Fiction', 'Fiction', 'CATEGORY', true, '#8B4789', '/media/categories/category_1.jpg'),
(2, 'Philosophy', 'Philosophie', 'CATEGORY', true, '#2C5F8D', '/media/categories/category_2.jpg'),
(3, 'Classic', 'Classique', 'CATEGORY', true, '#8B7355', '/media/categories/category_3.jpg'),
(4, 'Adventure', 'Aventure', 'CATEGORY', true, '#D97642', '/media/categories/category_4.jpg'),
(5, 'Romance', 'Romance', 'CATEGORY', true, '#E94B7E', '/media/categories/category_5.jpg'),
(6, 'Drama', 'Drame', 'CATEGORY', true, '#7D3C5D', '/media/categories/category_6.jpg'),
(7, 'Science Fiction', 'Science-Fiction', 'CATEGORY', true, '#4A90E2', '/media/categories/category_7.jpg'),
(8, 'Historical', 'Historique', 'CATEGORY', true, '#8B6914', '/media/categories/category_8.jpg'),
(9, 'Children''s Literature', 'Littérature Jeunesse', 'CATEGORY', true, '#F7B731', '/media/categories/category_9.jpg');

-- ETIQUETTE tags
INSERT INTO tag (id, name_en, name_fr, type, active, color_hex, image_url) VALUES
(11, 'Bestseller', 'Meilleures Ventes', 'ETIQUETTE', true, '#FFD700', '/media/categories/category_11.jpg'),
(12, 'New Release', 'Nouveauté', 'ETIQUETTE', true, '#32CD32', '/media/categories/category_12.jpg'),
(14, 'Staff Pick', 'Choix du Personnel', 'ETIQUETTE', true, '#9B59B6', '/media/categories/category_14.jpg'),
(15, 'Award Winner', 'Prix Littéraire', 'ETIQUETTE', true, '#E74C3C', '/media/categories/category_15.jpg'),
(16, 'On Sale', 'En Promotion', 'ETIQUETTE', true, '#FF6347', '/media/categories/category_16.jpg');

-- MAIN_DISPLAY tags
INSERT INTO tag (id, name_en, name_fr, type, active, color_hex, image_url) VALUES
(18, 'Featured', 'À la Une', 'MAIN_DISPLAY', true, '#E67E22', '/media/categories/category_18.jpg'),
(19, 'Popular', 'Populaire', 'MAIN_DISPLAY', true, '#3498DB', '/media/categories/category_19.jpg'),
(20, 'Recommended', 'Recommandé', 'MAIN_DISPLAY', true, '#1ABC9C', '/media/categories/category_20.jpg'),
(21, 'Trending', 'Tendance', 'MAIN_DISPLAY', true, '#F39C12', '/media/categories/category_21.jpg'),
(22, 'Must Read', 'À Lire Absolument', 'MAIN_DISPLAY', true, '#C0392B', '/media/categories/category_22.jpg'),
(23, 'Editor''s Choice', 'Choix de l''Éditeur', 'MAIN_DISPLAY', true, '#8E44AD', '/media/categories/category_23.jpg');

-- Update sequence to continue from the last inserted ID
SELECT setval('tag_seq', 23, true);
