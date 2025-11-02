-- Book-Tag Relationships
-- Constraints:
-- 1. Each category must have at least 3 books
-- 2. Each book must have at least 1 etiquette
-- 3. Three main displays are associated to all books

-- Categories (each with at least 3 books):
-- Fiction (1): books 3, 6, 8
-- Philosophy (2): books 4, 5, 10
-- Classic (3): books 3, 8, 9, 10
-- Adventure (4): books 6, 8
-- Romance (5): books 3, 8
-- Drama (6): books 3, 4, 9
-- Science Fiction (7): not used (no sci-fi books)
-- Historical (8): books 3, 8, 9
-- Children's Literature (9): books 6

INSERT INTO rel_tag__book (tag_id, book_id) VALUES
-- CATEGORIES

-- Fiction (1) - Les Misérables, Le Petit Prince, Le Comte de Monte-Cristo
(1, 3),
(1, 6),
(1, 8),

-- Philosophy (2) - L'Étranger, Le Deuxième Sexe, À la recherche du temps perdu
(2, 4),
(2, 5),
(2, 10),

-- Classic (3) - Les Misérables, Le Comte de Monte-Cristo, Germinal, À la recherche du temps perdu
(3, 3),
(3, 8),
(3, 9),
(3, 10),

-- Adventure (4) - Le Petit Prince, Le Comte de Monte-Cristo
(4, 6),
(4, 8),

-- Romance (5) - Les Misérables, Le Comte de Monte-Cristo
(5, 3),
(5, 8),

-- Drama (6) - Les Misérables, L'Étranger, Germinal
(6, 3),
(6, 4),
(6, 9),

-- Historical (8) - Les Misérables, Le Comte de Monte-Cristo, Germinal
(8, 3),
(8, 8),
(8, 9),

-- Children's Literature (9) - Le Petit Prince
(9, 6),

-- ETIQUETTES (each book gets at least 1)

-- Book 3: Les Misérables - Classic masterpiece
(15, 3), -- Award Winner
(11, 3), -- Bestseller

-- Book 4: L'Étranger - Existentialist classic
(15, 4), -- Award Winner
(14, 4), -- Staff Pick

-- Book 5: Le Deuxième Sexe - Feminist classic
(15, 5), -- Award Winner
(14, 5), -- Staff Pick

-- Book 6: Le Petit Prince - Popular children's book
(11, 6), -- Bestseller
(12, 6), -- New Release (fictional - for variety)

-- Book 8: Le Comte de Monte-Cristo - Adventure classic
(11, 8), -- Bestseller
(15, 8), -- Award Winner

-- Book 9: Germinal - Social drama classic
(15, 9), -- Award Winner
(14, 9), -- Staff Pick

-- Book 10: À la recherche du temps perdu - Literary masterpiece
(15, 10), -- Award Winner
(14, 10), -- Staff Pick

-- MAIN DISPLAYS (3 selected: Featured, Popular, Recommended - applied to all books)

-- Featured (18) - all books
(18, 3),
(18, 4),
(18, 5),
(18, 6),
(18, 8),
(18, 9),
(18, 10),

-- Popular (19) - all books
(19, 3),
(19, 4),
(19, 5),
(19, 6),
(19, 8),
(19, 9),
(19, 10),

-- Recommended (20) - all books
(20, 3),
(20, 4),
(20, 5),
(20, 6),
(20, 8),
(20, 9),
(20, 10);
