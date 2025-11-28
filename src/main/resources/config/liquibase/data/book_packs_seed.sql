-- Book Packs Seed Data
-- This script populates the book_pack table with sample data

-- Get the next sequence value for book_pack_seq
-- Note: Adjust starting value if needed based on existing data

-- Pack 1: Classic French Literature Bundle (Books: 3, 9, 16)
-- Les Misérables + Germinal + Les Trois Mousquetaires
-- Combined price: 1850 + 1600 + 1800 = 5250, Discount: 450 = 4800
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Collection Classiques Français',
        'Un voyage à travers les plus grands classiques de la littérature française. Cette collection regroupe trois chefs-d''œuvre incontournables qui ont marqué l''histoire littéraire : Les Misérables de Victor Hugo, un monument de la littérature humaniste, Germinal d''Émile Zola, roman naturaliste puissant sur la condition ouvrière, et Les Trois Mousquetaires d''Alexandre Dumas, une épopée captivante de cape et d''épée.',
        '/media/book-packs/pack_1.jpg',
        4800.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 2: Philosophy & Existentialism (Books: 4, 5, 13)
-- L'Étranger + Le Deuxième Sexe + La Nausée
-- Combined price: 1200 + 2500 + 1300 = 5000, Discount: 400 = 4600
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Philosophie & Existentialisme',
        'Plongez au cœur de la pensée existentialiste avec cette sélection exceptionnelle. L''Étranger d''Albert Camus explore l''absurdité de l''existence, Le Deuxième Sexe de Simone de Beauvoir révolutionne la pensée féministe, et La Nausée de Jean-Paul Sartre confronte le lecteur à la contingence et à l''absurde. Une collection essentielle pour comprendre les courants philosophiques du XXe siècle.',
        '/media/book-packs/pack_2.jpg',
        4600.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 3: Adventure & Discovery (Books: 6, 8, 17)
-- Le Petit Prince + Le Comte de Monte-Cristo + Le Tour du monde en quatre-vingts jours
-- Combined price: 950 + 2200 + 1150 = 4300, Discount: 350 = 3950
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Aventure & Découverte',
        'Embarquez pour un voyage extraordinaire à travers trois récits d''aventure inoubliables. Du conte philosophique du Petit Prince aux aventures palpitantes d''Edmond Dantès dans Le Comte de Monte-Cristo, en passant par le périple audacieux de Phileas Fogg dans Le Tour du monde en quatre-vingts jours. Cette collection ravira les amateurs d''évasion et de suspense.',
        '/media/book-packs/pack_3.png',
        3950.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 4: Drama & Romance (Books: 3, 12, 14)
-- Les Misérables + Madame Bovary + Notre-Dame de Paris
-- Combined price: 1850 + 1350 + 1750 = 4950, Discount: 500 = 4450
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Drames & Passions',
        'Une collection captivante explorant les méandres du cœur humain. Les Misérables de Victor Hugo dépeint la rédemption et la justice sociale, Madame Bovary de Gustave Flaubert analyse les désillusions romantiques, tandis que Notre-Dame de Paris nous plonge dans les amours impossibles du Paris médiéval. Des histoires puissantes qui ont traversé les siècles.',
        '/media/book-packs/pack_4.jpg',
        4450.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 5: Literary Masterpieces (Books: 8, 10, 15)
-- Le Comte de Monte-Cristo + À la recherche du temps perdu + La Peste
-- Combined price: 2200 + 1900 + 1250 = 5350, Discount: 550 = 4800
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Chefs-d''Œuvre Littéraires',
        'Les plus grands monuments de la littérature française réunis en une seule collection. Le Comte de Monte-Cristo d''Alexandre Dumas pour l''aventure et la vengeance, À la recherche du temps perdu de Marcel Proust pour l''introspection et la mémoire, et La Peste d''Albert Camus pour sa réflexion sur la condition humaine. Un trésor littéraire inestimable.',
        '/media/book-packs/pack_5.jpg',
        4800.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 6: Social Realism (Books: 9, 15, 18)
-- Germinal + La Peste + L'Assommoir
-- Combined price: 1600 + 1250 + 1550 = 4400, Discount: 350 = 4050
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Réalisme Social',
        'Trois romans puissants qui dénoncent les injustices et explorent la condition humaine. Germinal d''Émile Zola dépeint la lutte des mineurs, La Peste d''Albert Camus utilise l''épidémie comme allégorie de l''absurde, et L''Assommoir de Zola expose les ravages de l''alcoolisme et de la misère. Une lecture engagée et bouleversante.',
        '/media/book-packs/pack_6.jpg',
        4050.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 7: Literary Exploration (Books: 10, 11, 12)
-- À la recherche du temps perdu + Vingt mille lieues sous les mers + Madame Bovary
-- Combined price: 1900 + 1400 + 1350 = 4650, Discount: 400 = 4250
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Exploration Littéraire',
        'Un voyage fascinant à travers différents univers littéraires. De l''introspection proustienne d''À la recherche du temps perdu aux aventures sous-marines de Vingt mille lieues sous les mers, en passant par le drame psychologique de Madame Bovary. Cette collection offre une diversité exceptionnelle de styles et de thèmes.',
        '/media/book-packs/pack_7.jpg',
        4250.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 8: Victor Hugo Collection (Books: 3, 11, 16)
-- Les Misérables + Vingt mille lieues sous les mers + Les Trois Mousquetaires
-- Combined price: 1850 + 1400 + 1800 = 5050, Discount: 450 = 4600
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Collection Romans d''Aventure',
        'Les plus grandes épopées de la littérature française. Des barricades parisiennes des Misérables aux profondeurs océaniques de Vingt mille lieues sous les mers, jusqu''aux duels des mousquetaires. Cette collection réunit l''action, le suspense et les grandes causes qui ont fait vibrer des générations de lecteurs.',
        '/media/book-packs/pack_8.jpg',
        4600.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 9: Albert Camus Essentials (Books: 4, 12, 17, 18)
-- L'Étranger + Madame Bovary + Le Tour du monde en quatre-vingts jours + L'Assommoir
-- Combined price: 1200 + 1350 + 1150 + 1550 = 5250, Discount: 500 = 4750
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Classiques Intemporels',
        'Une sélection éclectique des plus beaux romans de la littérature française. De l''absurde de L''Étranger aux désillusions de Madame Bovary, de l''aventure du Tour du monde en quatre-vingts jours au naturalisme de L''Assommoir. Quatre perspectives différentes sur la condition humaine et la société.',
        '/media/book-packs/pack_9.jpg',
        4750.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 10: Starter Pack (Books: 6, 13, 17)
-- Le Petit Prince + La Nausée + Le Tour du monde en quatre-vingts jours
-- Combined price: 950 + 1300 + 1150 = 3400, Discount: 250 = 3150
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Pack Découverte Littéraire',
        'Le point de départ idéal pour les nouveaux lecteurs de littérature française. Le Petit Prince pour la poésie et la philosophie accessible, La Nausée pour découvrir l''existentialisme, et Le Tour du monde en quatre-vingts jours pour l''aventure pure. Trois styles, trois approches, une introduction parfaite à la grande littérature.',
        '/media/book-packs/pack_10.png',
        3150.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 11: Historical Epics (Books: 8, 14, 16)
-- Le Comte de Monte-Cristo + Notre-Dame de Paris + Les Trois Mousquetaires
-- Combined price: 2200 + 1750 + 1800 = 5750, Discount: 600 = 5150
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Épopées Historiques',
        'Voyagez à travers l''histoire de France avec ces trois romans légendaires. Le Comte de Monte-Cristo et sa vengeance implacable, Notre-Dame de Paris et son Paris médiéval gothique, Les Trois Mousquetaires et leurs aventures au service du roi. Des fresques historiques inoubliables pleines d''action et de rebondissements.',
        '/media/book-packs/pack_11.jpg',
        5150.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 12: Deep Reflections (Books: 5, 9, 10, 15)
-- Le Deuxième Sexe + Germinal + À la recherche du temps perdu + La Peste
-- Combined price: 2500 + 1600 + 1900 + 1250 = 7250, Discount: 700 = 6550
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Réflexions Profondes',
        'La collection ultime pour les esprits curieux et les âmes contemplatives. Le Deuxième Sexe révolutionne la pensée sur le genre, Germinal dénonce l''injustice sociale, À la recherche du temps perdu explore la mémoire et l''identité, tandis que La Peste questionne notre rapport à l''absurde. Quatre œuvres majeures pour une réflexion profonde sur l''humanité.',
        '/media/book-packs/pack_12.jpg',
        6550.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Now insert the relationships between book packs and books
-- Pack 1: Books 3, 9, 16
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (1, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (1, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (1, 16);

-- Pack 2: Books 4, 5, 13
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (2, 4);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (2, 5);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (2, 13);

-- Pack 3: Books 6, 8, 17
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (3, 6);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (3, 8);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (3, 17);

-- Pack 4: Books 3, 12, 14
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (4, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (4, 12);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (4, 14);

-- Pack 5: Books 8, 10, 15
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (5, 8);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (5, 10);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (5, 15);

-- Pack 6: Books 9, 15, 18
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (6, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (6, 15);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (6, 18);

-- Pack 7: Books 10, 11, 12
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (7, 10);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (7, 11);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (7, 12);

-- Pack 8: Books 3, 11, 16
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (8, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (8, 11);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (8, 16);

-- Pack 9: Books 4, 12, 17, 18
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (9, 4);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (9, 12);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (9, 17);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (9, 18);

-- Pack 10: Books 6, 13, 17
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (10, 6);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (10, 13);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (10, 17);

-- Pack 11: Books 8, 14, 16
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (11, 8);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (11, 14);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (11, 16);

-- Pack 12: Books 5, 9, 10, 15
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (12, 5);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (12, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (12, 10);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (12, 15);
