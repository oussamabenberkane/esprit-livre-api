-- Book Packs Seed Data - Second Batch
-- This script populates 12 additional book packs

-- Pack 13: French Literature Essentials (Books: 4, 6, 10)
-- L'Étranger + Le Petit Prince + À la recherche du temps perdu
-- Combined price: 1200 + 950 + 1900 = 4050, Discount: 300 = 3750
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Essentiels de la Littérature Française',
        'Trois piliers incontournables de la littérature française moderne. L''Étranger de Camus pour découvrir l''absurde, Le Petit Prince de Saint-Exupéry pour retrouver l''émerveillement de l''enfance, et À la recherche du temps perdu de Proust pour explorer les méandres de la mémoire. Une initiation parfaite aux grandes œuvres françaises.',
        '/media/book-packs/pack_13.jpg',
        3750.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 14: Zola Collection (Books: 9, 18)
-- Germinal + L'Assommoir
-- Combined price: 1600 + 1550 = 3150, Discount: 200 = 2950
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Collection Émile Zola',
        'Plongez dans l''univers naturaliste d''Émile Zola avec deux de ses romans les plus puissants. Germinal dépeint la lutte héroïque des mineurs contre l''oppression, tandis que L''Assommoir expose les ravages de l''alcoolisme dans le Paris ouvrier. Deux fresques sociales bouleversantes qui dénoncent les injustices de leur époque.',
        '/media/book-packs/pack_14.jpg',
        2950.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 15: Romance & Tragedy (Books: 3, 8, 12, 14)
-- Les Misérables + Le Comte de Monte-Cristo + Madame Bovary + Notre-Dame de Paris
-- Combined price: 1850 + 2200 + 1350 + 1750 = 7150, Discount: 650 = 6500
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Romance & Tragédie',
        'Les plus grandes histoires d''amour et de tragédie de la littérature française réunies. Des barricades des Misérables aux machinations du Comte de Monte-Cristo, des désillusions de Madame Bovary aux amours impossibles de Notre-Dame de Paris. Quatre romans épiques qui explorent les passions humaines dans toute leur complexité.',
        '/media/book-packs/pack_15.jpg',
        6500.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 16: Young Readers Pack (Books: 6, 11, 17)
-- Le Petit Prince + Vingt mille lieues sous les mers + Le Tour du monde en quatre-vingts jours
-- Combined price: 950 + 1400 + 1150 = 3500, Discount: 250 = 3250
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Collection Jeunesse Aventure',
        'Des aventures extraordinaires pour les jeunes lecteurs et les cœurs d''enfant. Le Petit Prince et son voyage philosophique entre les planètes, Vingt mille lieues sous les mers et les mystères des profondeurs océaniques, Le Tour du monde en quatre-vingts jours et son pari audacieux. Trois classiques intemporels qui éveillent l''imagination.',
        '/media/book-packs/pack_16.png',
        3250.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 17: Philosophical Minds (Books: 4, 5, 10, 13, 15)
-- L'Étranger + Le Deuxième Sexe + À la recherche du temps perdu + La Nausée + La Peste
-- Combined price: 1200 + 2500 + 1900 + 1300 + 1250 = 8150, Discount: 800 = 7350
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Esprits Philosophiques',
        'La collection ultime pour les penseurs et les chercheurs de sens. De l''absurde camusien dans L''Étranger et La Peste, à la révolution féministe du Deuxième Sexe, en passant par l''existentialisme de La Nausée et l''introspection proustienne. Cinq œuvres majeures qui ont façonné la pensée du XXe siècle.',
        '/media/book-packs/pack_17.jpg',
        7350.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 18: Historical Fiction (Books: 3, 9, 14, 16)
-- Les Misérables + Germinal + Notre-Dame de Paris + Les Trois Mousquetaires
-- Combined price: 1850 + 1600 + 1750 + 1800 = 7000, Discount: 700 = 6300
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Romans Historiques',
        'Voyagez à travers les époques de l''histoire française avec ces quatre monuments littéraires. Du Paris médiéval de Notre-Dame aux barricades de 1832 dans Les Misérables, des mines du Nord dans Germinal aux intrigues de cour des Trois Mousquetaires. L''histoire de France racontée par ses plus grands auteurs.',
        '/media/book-packs/pack_18.jpg',
        6300.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 19: Love & Society (Books: 5, 12, 18)
-- Le Deuxième Sexe + Madame Bovary + L'Assommoir
-- Combined price: 2500 + 1350 + 1550 = 5400, Discount: 450 = 4950
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Amour & Société',
        'Trois œuvres puissantes qui analysent la condition féminine et les contraintes sociales. Le Deuxième Sexe révolutionne la pensée féministe, Madame Bovary dépeint les désillusions d''une femme prisonnière de son milieu, L''Assommoir expose les ravages de la pauvreté sur les femmes ouvrières. Une réflexion profonde sur la place des femmes dans la société.',
        '/media/book-packs/pack_19.jpg',
        4950.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 20: Adventure Classics (Books: 8, 11, 16, 17)
-- Le Comte de Monte-Cristo + Vingt mille lieues sous les mers + Les Trois Mousquetaires + Le Tour du monde en quatre-vingts jours
-- Combined price: 2200 + 1400 + 1800 + 1150 = 6550, Discount: 600 = 5950
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Classiques de l''Aventure',
        'Les plus grandes aventures jamais écrites réunies en une collection exceptionnelle. De la vengeance méthodique du Comte de Monte-Cristo aux explorations sous-marines du capitaine Nemo, des duels des mousquetaires au défi audacieux de Phileas Fogg. Quatre épopées qui ont captivé des générations de lecteurs.',
        '/media/book-packs/pack_20.jpg',
        5950.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 21: Literary Giants (Books: 3, 4, 9, 10)
-- Les Misérables + L'Étranger + Germinal + À la recherche du temps perdu
-- Combined price: 1850 + 1200 + 1600 + 1900 = 6550, Discount: 550 = 6000
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Géants Littéraires',
        'Quatre monuments de la littérature française qui ont marqué l''histoire littéraire mondiale. Les Misérables et son humanisme bouleversant, L''Étranger et son absurde désarmant, Germinal et son réalisme social, À la recherche du temps perdu et son introspection magistrale. Les œuvres qui ont redéfini la littérature.',
        '/media/book-packs/pack_21.jpg',
        6000.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 22: Compact Classics (Books: 4, 13, 15)
-- L'Étranger + La Nausée + La Peste
-- Combined price: 1200 + 1300 + 1250 = 3750, Discount: 300 = 3450
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Classiques Compacts',
        'Trois romans courts mais puissants de la philosophie existentialiste. L''Étranger et son regard détaché sur l''absurdité, La Nausée et sa confrontation avec la contingence, La Peste et sa méditation sur la condition humaine face à l''épidémie. Des lectures accessibles mais profondes, parfaites pour une première approche de l''existentialisme.',
        '/media/book-packs/pack_22.jpg',
        3450.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 23: Epic Tales (Books: 3, 8, 10, 16)
-- Les Misérables + Le Comte de Monte-Cristo + À la recherche du temps perdu + Les Trois Mousquetaires
-- Combined price: 1850 + 2200 + 1900 + 1800 = 7750, Discount: 750 = 7000
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Épopées Monumentales',
        'Les plus grandes fresques romanesques de la littérature française. Quatre œuvres monumentales qui tissent des récits complexes sur des centaines de pages. Des bas-fonds de Paris aux châteaux d''If, des salons mondains proustiens aux champs de bataille des mousquetaires. Pour les lecteurs passionnés prêts à s''immerger dans de véritables univers littéraires.',
        '/media/book-packs/pack_23.jpg',
        7000.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Pack 24: Social Commentary (Books: 5, 9, 15, 18)
-- Le Deuxième Sexe + Germinal + La Peste + L'Assommoir
-- Combined price: 2500 + 1600 + 1250 + 1550 = 6900, Discount: 650 = 6250
INSERT INTO book_pack (id, title, description, cover_url, price, created_date, last_modified_date)
VALUES (nextval('book_pack_seq'),
        'Critique Sociale',
        'Quatre œuvres engagées qui dénoncent les injustices et interrogent la société. Le Deuxième Sexe analyse l''oppression des femmes, Germinal expose l''exploitation des ouvriers, La Peste questionne notre rapport au mal collectif, L''Assommoir dévoile les ravages de la misère urbaine. Une collection essentielle pour comprendre les luttes sociales à travers la littérature.',
        '/media/book-packs/pack_24.jpg',
        6250.00,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

-- Now insert the relationships between book packs and books

-- Pack 13: Books 4, 6, 10
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (13, 4);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (13, 6);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (13, 10);

-- Pack 14: Books 9, 18
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (14, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (14, 18);

-- Pack 15: Books 3, 8, 12, 14
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (15, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (15, 8);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (15, 12);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (15, 14);

-- Pack 16: Books 6, 11, 17
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (16, 6);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (16, 11);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (16, 17);

-- Pack 17: Books 4, 5, 10, 13, 15
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (17, 4);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (17, 5);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (17, 10);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (17, 13);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (17, 15);

-- Pack 18: Books 3, 9, 14, 16
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (18, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (18, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (18, 14);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (18, 16);

-- Pack 19: Books 5, 12, 18
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (19, 5);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (19, 12);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (19, 18);

-- Pack 20: Books 8, 11, 16, 17
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (20, 8);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (20, 11);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (20, 16);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (20, 17);

-- Pack 21: Books 3, 4, 9, 10
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (21, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (21, 4);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (21, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (21, 10);

-- Pack 22: Books 4, 13, 15
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (22, 4);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (22, 13);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (22, 15);

-- Pack 23: Books 3, 8, 10, 16
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (23, 3);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (23, 8);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (23, 10);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (23, 16);

-- Pack 24: Books 5, 9, 15, 18
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (24, 5);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (24, 9);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (24, 15);
INSERT INTO rel_book_pack__books (book_pack_id, books_id) VALUES (24, 18);
