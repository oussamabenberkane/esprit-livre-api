-- Esprit Livre Database Seed Data
-- This script populates the database with sample data for development and testing

-- Clear existing data (optional - comment out if you want to preserve existing data)
-- TRUNCATE TABLE order_item, jhi_order, jhi_like, book_pack_books, tag_book, book_pack, book, tag, author CASCADE;

-- ============================================
-- AUTHORS (15 authors)
-- ============================================
INSERT INTO author (id, name, profile_picture_url, created_by, created_date, last_modified_by, last_modified_date) VALUES
(1, 'Yasmina Khadra', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(2, 'Amin Maalouf', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(3, 'Khaled Hosseini', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(4, 'Gabriel García Márquez', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(5, 'Chimamanda Ngozi Adichie', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(6, 'Haruki Murakami', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(7, 'Yuval Noah Harari', 'https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(8, 'Taha Hussein', 'https://images.unsplash.com/photo-1463453091185-61582044d556?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(9, 'Naguib Mahfouz', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(10, 'Assia Djebar', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(11, 'Tahar Ben Jelloun', 'https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(12, 'Albert Camus', 'https://images.unsplash.com/photo-1552058544-f2b08422138a?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(13, 'Kateb Yacine', 'https://images.unsplash.com/photo-1546961329-78bef0414d7c?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(14, 'Malek Bennabi', 'https://images.unsplash.com/photo-1531427186611-ecfd6d936c79?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(15, 'Mohamed Dib', 'https://images.unsplash.com/photo-1528892952291-009c663ce843?w=400', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

-- ============================================
-- TAGS (15 Category Tags + 3 Main Display Tags + Etiquettes)
-- ============================================
INSERT INTO tag (id, name_en, name_fr, type, active, color_hex, image_url) VALUES
-- 15 CATEGORY TAGS
(1, 'Fiction', 'Fiction', 'CATEGORY', true, '#FF6B6B', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400'),
(2, 'Non-Fiction', 'Non-Fiction', 'CATEGORY', true, '#4ECDC4', 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400'),
(3, 'Children & Young Adult', 'Jeunesse', 'CATEGORY', true, '#FFE66D', 'https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?w=400'),
(4, 'Academic & Education', 'Académique', 'CATEGORY', true, '#95E1D3', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=400'),
(5, 'Self-Help & Development', 'Développement Personnel', 'CATEGORY', true, '#A8E6CF', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400'),
(6, 'History & Biography', 'Histoire & Biographie', 'CATEGORY', true, '#B4A7D6', 'https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=400'),
(7, 'Science & Technology', 'Science & Technologie', 'CATEGORY', true, '#A2D2FF', 'https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=400'),
(8, 'Philosophy & Religion', 'Philosophie & Religion', 'CATEGORY', true, '#CDB4DB', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400'),
(9, 'Arts & Culture', 'Arts & Culture', 'CATEGORY', true, '#FFAFCC', 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=400'),
(10, 'Business & Economics', 'Business & Économie', 'CATEGORY', true, '#FFC8DD', 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400'),
(11, 'Poetry & Drama', 'Poésie & Théâtre', 'CATEGORY', true, '#BDE0FE', 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=400'),
(12, 'Mystery & Thriller', 'Policier & Thriller', 'CATEGORY', true, '#FFDAB9', 'https://images.unsplash.com/photo-1526243741027-444d633d7365?w=400'),
(13, 'Romance', 'Romance', 'CATEGORY', true, '#F4ACB7', 'https://images.unsplash.com/photo-1474552226712-ac0f0961a954?w=400'),
(14, 'Science Fiction & Fantasy', 'SF & Fantasy', 'CATEGORY', true, '#9D84B7', 'https://images.unsplash.com/photo-1534105615374-395cfdaa9f86?w=400'),
(15, 'Travel & Adventure', 'Voyage & Aventure', 'CATEGORY', true, '#B5E7A0', 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400'),

-- 3 MAIN DISPLAY TAGS
(16, 'Bestsellers', 'Meilleures Ventes', 'MAIN_DISPLAY', true, '#F38181', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=800'),
(17, 'New Arrivals', 'Nouveautés', 'MAIN_DISPLAY', true, '#AA96DA', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=800'),
(18, 'Staff Picks', 'Coups de Coeur', 'MAIN_DISPLAY', true, '#6C5CE7', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800'),

-- ETIQUETTE TAGS
(19, 'Discounted', 'En Promotion', 'ETIQUETTE', true, '#FCBAD3', null),
(20, 'Classic', 'Classique', 'ETIQUETTE', true, '#A8D8EA', null),
(21, 'Award Winner', 'Primé', 'ETIQUETTE', true, '#FFD93D', null);

-- ============================================
-- BOOKS (15 books, 5 with stock_quantity = 0)
-- ============================================
INSERT INTO book (id, title, price, stock_quantity, cover_image_url, description, active, language, author_id, created_at, updated_at) VALUES
-- Book 1 (In Stock)
(1, 'Les Hirondelles de Kaboul', 32.50, 25, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400',
'Un roman poignant sur l''amour et la guerre dans le Kaboul des Talibans. Une histoire bouleversante qui explore la condition humaine dans les moments les plus sombres.',
true, 'FR', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 2 (OUT OF STOCK)
(2, 'Léon l''Africain', 28.00, 0, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400',
'L''extraordinaire épopée d''un homme entre deux mondes, entre Orient et Occident. Un voyage fascinant à travers le XVIe siècle méditerranéen.',
true, 'FR', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 3 (In Stock)
(3, 'The Kite Runner', 24.99, 30, 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400',
'A powerful story of friendship, betrayal, and redemption set against the backdrop of Afghanistan. An unforgettable tale of the bonds between fathers and sons.',
true, 'EN', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 4 (In Stock)
(4, 'One Hundred Years of Solitude', 45.50, 22, 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=400',
'The multi-generational story of the Buendía family in the fictional town of Macondo. A masterpiece of magical realism that has captivated readers worldwide.',
true, 'EN', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 5 (OUT OF STOCK)
(5, 'Americanah', 39.75, 0, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400',
'A powerful love story about race, identity, and belonging in 21st century America and Nigeria. A brilliantly written contemporary novel.',
true, 'EN', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 6 (In Stock)
(6, 'Norwegian Wood', 21.99, 20, 'https://images.unsplash.com/photo-1533327325824-76bc4e62d560?w=400',
'A nostalgic story of loss and burgeoning sexuality set in late 1960s Tokyo. A coming-of-age tale that explores love, death, and memory.',
true, 'EN', 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 7 (In Stock)
(7, 'Sapiens: A Brief History of Humankind', 55.00, 45, 'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=400',
'From the evolution of Homo sapiens to the present day, this book explores how we came to dominate the world. A thought-provoking journey through human history.',
true, 'EN', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 8 (OUT OF STOCK)
(8, 'Homo Deus: A Brief History of Tomorrow', 48.50, 0, 'https://images.unsplash.com/photo-1491841573634-28140fc7ced7?w=400',
'What will happen to us when artificial intelligence outperforms humans? A fascinating exploration of humanity''s future challenges and possibilities.',
true, 'EN', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 9 (In Stock)
(9, 'Les Identités Meurtrières', 19.90, 32, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=400',
'Une réflexion profonde sur l''identité, l''appartenance et les conflits qui en découlent. Un essai essentiel pour comprendre notre monde moderne.',
true, 'FR', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 10 (In Stock)
(10, 'الأيام', 15.50, 40, 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400',
'السيرة الذاتية لطه حسين، رحلة ملهمة من الطفولة في صعيد مصر إلى الأزهر والسوربون. عمل أدبي خالد يروي قصة التحدي والإصرار.',
true, 'AR', 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 11 (In Stock)
(11, 'في الشعر الجاهلي', 18.00, 25, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400',
'دراسة نقدية جريئة للشعر الجاهلي أثارت جدلاً واسعاً. كتاب مهم في تاريخ الأدب العربي الحديث.',
true, 'AR', 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 12 (OUT OF STOCK)
(12, 'الثلاثية', 62.00, 0, 'https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?w=400',
'الثلاثية الشهيرة لنجيب محفوظ: بين القصرين، قصر الشوق، والسكرية. ملحمة أدبية تصور الحياة المصرية عبر أجيال.',
true, 'AR', 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 13 (In Stock)
(13, 'L''Étranger', 16.50, 50, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400',
'Un chef-d''œuvre de la littérature existentialiste. L''histoire de Meursault, un homme ordinaire confronté à l''absurdité de l''existence.',
true, 'FR', 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 14 (In Stock)
(14, 'La Grande Maison', 27.00, 35, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=400',
'Un roman magistral sur la mémoire collective et l''identité algérienne. Une œuvre majeure de la littérature maghrébine.',
true, 'FR', 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Book 15 (In Stock)
(15, 'Nedjma', 22.50, 28, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400',
'Un roman révolutionnaire qui a marqué la littérature algérienne. Une quête poétique et politique à travers l''Algérie coloniale.',
true, 'FR', 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- TAG-BOOK RELATIONSHIPS
-- ============================================
-- Each of the 3 Main Display Tags has 10 books
INSERT INTO rel_tag__book (tag_id, book_id) VALUES
-- Bestsellers (tag 16) - 10 books
(16, 1), (16, 3), (16, 4), (16, 7), (16, 10), (16, 12), (16, 13), (16, 6), (16, 9), (16, 14),

-- New Arrivals (tag 17) - 10 books
(17, 2), (17, 5), (17, 8), (17, 11), (17, 15), (17, 1), (17, 4), (17, 7), (17, 10), (17, 13),

-- Staff Picks (tag 18) - 10 books
(18, 3), (18, 4), (18, 6), (18, 9), (18, 12), (18, 13), (18, 14), (18, 15), (18, 2), (18, 5),

-- Category Tags - Distribution across books
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 13), (1, 14), (1, 15),  -- Fiction
(2, 7), (2, 8), (2, 9), (2, 10), (2, 11),  -- Non-Fiction
(6, 10), (6, 11), (6, 12),  -- History & Biography
(8, 9), (8, 10), (8, 11), (8, 14),  -- Philosophy & Religion
(11, 15),  -- Poetry & Drama
(12, 1), (12, 3),  -- Mystery & Thriller
(13, 5), (13, 6),  -- Romance
(7, 7), (7, 8),  -- Science & Technology
(4, 11),  -- Academic
(9, 12), (9, 14), (9, 15),  -- Arts & Culture
(15, 2), (15, 3),  -- Travel & Adventure
(10, 7), (10, 8), (10, 9),  -- Business & Economics

-- Etiquette Tags
(19, 1), (19, 10), (19, 13),  -- Discounted
(20, 4), (20, 12), (20, 13), (20, 15),  -- Classic
(21, 4), (21, 7), (21, 12);  -- Award Winner

-- ============================================
-- BOOK PACKS (15 packs)
-- ============================================
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date) VALUES
(1, 'Collection Yuval Noah Harari',
'Découvrez la collection complète de Yuval Noah Harari : Sapiens et Homo Deus. Une exploration fascinante de l''histoire de l''humanité et de son avenir.',
'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=600',
89.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 'Classiques de la Littérature Mondiale',
'Une sélection des plus grands chefs-d''œuvre : Cent ans de solitude et Le Kite Runner. Des histoires intemporelles.',
'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600',
62.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 'Pack Découverte Romance',
'Plongez dans des histoires d''amour captivantes avec Americanah et Norwegian Wood.',
'https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?w=600',
54.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(4, 'Pack Philosophie Existentialiste',
'Explorez l''existentialisme avec L''Étranger de Camus et Les Identités Meurtrières de Maalouf.',
'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600',
32.90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, 'Collection Littérature Algérienne',
'Découvrez les grands auteurs algériens : Nedjma, La Grande Maison et Les Hirondelles de Kaboul.',
'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=600',
74.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(6, 'Pack Taha Hussein - Œuvres Complètes',
'Les deux œuvres majeures de Taha Hussein : الأيام et في الشعر الجاهلي.',
'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600',
29.90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(7, 'Collection Naguib Mahfouz',
'La trilogie du Caire de Naguib Mahfouz, prix Nobel de littérature.',
'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600',
55.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(8, 'Pack Amin Maalouf',
'Deux œuvres magistrales d''Amin Maalouf : Léon l''Africain et Les Identités Meurtrières.',
'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600',
42.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(9, 'Pack Littérature Japonaise',
'Découvrez le Japon à travers Norwegian Wood de Haruki Murakami.',
'https://images.unsplash.com/photo-1533327325824-76bc4e62d560?w=600',
19.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(10, 'Pack Littérature Engagée',
'Des œuvres qui questionnent notre société : Americanah et Les Hirondelles de Kaboul.',
'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600',
64.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(11, 'Collection Réflexions Contemporaines',
'Pensez différemment avec Sapiens, Homo Deus et Les Identités Meurtrières.',
'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600',
112.90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(12, 'Pack Khaled Hosseini',
'L''œuvre émouvante de Khaled Hosseini : The Kite Runner.',
'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600',
22.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(13, 'Pack Grands Classiques Français',
'Les incontournables de la littérature française : L''Étranger et La Grande Maison.',
'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600',
39.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(14, 'Collection Réalisme Magique',
'Entrez dans des mondes extraordinaires avec Cent ans de solitude.',
'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=600',
42.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(15, 'Pack Littérature Africaine Moderne',
'Découvrez des voix africaines contemporaines : Americanah de Chimamanda Ngozi Adichie.',
'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600',
36.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- BOOK PACK - BOOKS RELATIONSHIPS
-- ============================================
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES
-- Pack 1: Harari Collection
(1, 7), (1, 8),
-- Pack 2: World Classics
(2, 4), (2, 3),
-- Pack 3: Romance
(3, 5), (3, 6),
-- Pack 4: Existentialism
(4, 13), (4, 9),
-- Pack 5: Algerian Literature
(5, 15), (5, 14), (5, 1),
-- Pack 6: Taha Hussein
(6, 10), (6, 11),
-- Pack 7: Mahfouz
(7, 12),
-- Pack 8: Maalouf
(8, 2), (8, 9),
-- Pack 9: Japanese Literature
(9, 6),
-- Pack 10: Engaged Literature
(10, 5), (10, 1),
-- Pack 11: Contemporary Reflections
(11, 7), (11, 8), (11, 9),
-- Pack 12: Hosseini
(12, 3),
-- Pack 13: French Classics
(13, 13), (13, 14),
-- Pack 14: Magic Realism
(14, 4),
-- Pack 15: Modern African Literature
(15, 5);

-- ============================================
-- SAMPLE ORDERS
-- ============================================
INSERT INTO jhi_order (id, unique_id, status, total_amount, shipping_cost, shipping_provider, shipping_method,
                       full_name, phone, email, street_address, wilaya, city, postal_code, created_at, updated_at) VALUES
-- Delivered Order
(1, 'ORD-2025-001', 'DELIVERED', 97.50, 5.00, 'YALIDINE', 'HOME_DELIVERY',
 'Ahmed Benali', '+213555123456', 'ahmed.benali@email.dz', '15 Rue Didouche Mourad', 'Alger', 'Alger Centre', '16000',
 CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),

-- Shipped Order
(2, 'ORD-2025-002', 'SHIPPED', 89.99, 4.00, 'ZR', 'SHIPPING_PROVIDER',
 'Fatima Zohra', '+213661234567', 'fatima.zohra@email.dz', '42 Boulevard Mohamed V', 'Oran', 'Oran', '31000',
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Confirmed Order
(3, 'ORD-2025-003', 'CONFIRMED', 78.49, 4.50, 'YALIDINE', 'HOME_DELIVERY',
 'Karim Mansouri', '+213771234567', 'karim.m@email.dz', '8 Rue Larbi Ben M''hidi', 'Constantine', 'Constantine', '25000',
 CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Pending Order
(4, 'ORD-2025-004', 'PENDING', 62.99, 4.00, 'YALIDINE', 'HOME_DELIVERY',
 'Samira Hamdi', '+213551122334', 'samira.h@email.dz', '23 Avenue de l''Indépendance', 'Annaba', 'Annaba', '23000',
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Cancelled Order
(5, 'ORD-2025-005', 'CANCELLED', 32.50, 3.50, 'ZR', 'SHIPPING_PROVIDER',
 'Youcef Brahimi', '+213661998877', 'youcef.b@email.dz', '56 Rue des Frères Bouadou', 'Blida', 'Blida', '09000',
 CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '9 days'),

-- Another Delivered Order
(6, 'ORD-2025-006', 'DELIVERED', 42.50, 3.00, 'YALIDINE', 'HOME_DELIVERY',
 'Nabila Kaci', '+213771556677', 'nabila.k@email.dz', '12 Cité des Jardins', 'Tizi Ouzou', 'Tizi Ouzou', '15000',
 CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '12 days');

-- ============================================
-- ORDER ITEMS
-- ============================================
INSERT INTO order_item (id, quantity, unit_price, total_price, item_type, order_id, book_id, book_pack_id) VALUES
-- Order 1 Items (Delivered: Books)
(1, 2, 32.50, 65.00, 'BOOK', 1, 1, null),  -- Les Hirondelles de Kaboul x2
(2, 1, 28.00, 28.00, 'BOOK', 1, 2, null),  -- Léon l'Africain x1

-- Order 2 Items (Shipped: Book Pack)
(3, 1, 89.99, 89.99, 'PACK', 2, null, 1),  -- Harari Collection Pack

-- Order 3 Items (Confirmed: Mixed)
(4, 1, 55.00, 55.00, 'BOOK', 3, 7, null),  -- Sapiens
(5, 1, 15.50, 15.50, 'BOOK', 3, 10, null), -- الأيام

-- Order 4 Items (Pending: Book Pack)
(6, 1, 62.99, 62.99, 'PACK', 4, null, 2),  -- World Classics Pack

-- Order 5 Items (Cancelled)
(7, 1, 32.50, 32.50, 'BOOK', 5, 1, null),  -- Les Hirondelles de Kaboul

-- Order 6 Items (Delivered: Pack)
(8, 1, 42.50, 42.50, 'PACK', 6, null, 8); -- Maalouf Pack

-- ============================================
-- UPDATE SEQUENCES
-- ============================================
SELECT setval('author_seq', (SELECT MAX(id) FROM author));
SELECT setval('tag_seq', (SELECT MAX(id) FROM tag));
SELECT setval('book_seq', (SELECT MAX(id) FROM book));
SELECT setval('book_pack_seq', (SELECT MAX(id) FROM book_pack));
SELECT setval('order_item_seq', (SELECT MAX(id) FROM order_item));
SELECT setval('order_seq', (SELECT MAX(id) FROM jhi_order));

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
SELECT 'Authors' as entity, COUNT(*) as count FROM author
UNION ALL SELECT 'Books', COUNT(*) FROM book
UNION ALL SELECT 'Books with stock_quantity = 0', COUNT(*) FROM book WHERE stock_quantity = 0
UNION ALL SELECT 'Tags (Total)', COUNT(*) FROM tag
UNION ALL SELECT 'Tags (CATEGORY)', COUNT(*) FROM tag WHERE type = 'CATEGORY'
UNION ALL SELECT 'Tags (MAIN_DISPLAY)', COUNT(*) FROM tag WHERE type = 'MAIN_DISPLAY'
UNION ALL SELECT 'Book Packs', COUNT(*) FROM book_pack
UNION ALL SELECT 'Orders', COUNT(*) FROM jhi_order
UNION ALL SELECT 'Order Items', COUNT(*) FROM order_item;

-- Verify Main Display Tags have 10 books each
SELECT t.name_en as tag_name, COUNT(tb.book_id) as book_count
FROM tag t
LEFT JOIN rel_tag__book tb ON t.id = tb.tag_id
WHERE t.type = 'MAIN_DISPLAY'
GROUP BY t.id, t.name_en;
