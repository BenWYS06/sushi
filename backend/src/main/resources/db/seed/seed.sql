\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.users') IS NULL
       OR to_regclass('public.products') IS NULL
       OR to_regclass('public.orders') IS NULL
       OR NOT EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'products'
             AND column_name = 'slug'
       ) THEN
        RAISE EXCEPTION 'Database schema is not ready. Start the backend once so Flyway can apply all migrations.';
    END IF;
END
$$;

TRUNCATE TABLE
    review_replies,
    reviews,
    payments,
    order_items,
    orders,
    promotion_products,
    promotions,
    product_images,
    tokens,
    audit_logs,
    products,
    users
RESTART IDENTITY CASCADE;

-- Passwords for the first three demo accounts remain the ones documented in README.
-- All other customer accounts use the same BCrypt hash as user@test.com (password: user).
INSERT INTO users
    (name, email, password, phone, user_role, city, street, house, apartment,
     email_verified, token_version, created_at, updated_at, created_by, updated_by)
VALUES
    ('Test User', 'user@test.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230001', 'CUSTOMER', 'Kyiv', 'Khreshchatyk', '12', '4A', true, 0, NOW() - INTERVAL '420 days', NOW() - INTERVAL '8 days', 'seed', 'seed'),
    ('Admin', 'admin@test.com', '$2a$12$hPwjJfuUU7oG8KLBxWh23ulaVS6PyX2w7YvnJK8gIOyMYaHk24bfq', '+380501230002', 'ADMIN', 'Kyiv', 'Velyka Vasylkivska', '18', NULL, true, 0, NOW() - INTERVAL '420 days', NOW() - INTERVAL '2 days', 'seed', 'seed'),
    ('Courier', 'courier@test.com', '$2a$12$03vzvEtpaBM3Un4yjj0nweW9qMYVZrd2axRIatRRmTk5dLiL95bgW', '+380501230003', 'COURIER', 'Kyiv', 'Sichovykh Striltsiv', '31', NULL, true, 0, NOW() - INTERVAL '390 days', NOW() - INTERVAL '1 day', 'seed', 'seed'),
    ('Oleksandr Kovalenko', 'oleksandr.kovalenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230004', 'CUSTOMER', 'Kyiv', 'Antonovycha', '45', '8B', true, 0, NOW() - INTERVAL '360 days', NOW() - INTERVAL '15 days', 'seed', 'seed'),
    ('Anna Shevchenko', 'anna.shevchenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230005', 'CUSTOMER', 'Kyiv', 'Peremohy Avenue', '102', '12', true, 0, NOW() - INTERVAL '345 days', NULL, 'seed', NULL),
    ('Maksym Bondarenko', 'maksym.bondarenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230006', 'CUSTOMER', 'Kyiv', 'Podilskyi Descent', '77', NULL, true, 1, NOW() - INTERVAL '330 days', NOW() - INTERVAL '30 days', 'seed', 'seed'),
    ('Sofia Melnyk', 'sofia.melnyk@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230007', 'CUSTOMER', 'Irpin', 'Universytetska', '21', '6C', true, 0, NOW() - INTERVAL '315 days', NULL, 'seed', NULL),
    ('Dmytro Tkachenko', 'dmytro.tkachenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230008', 'CUSTOMER', 'Kyiv', 'Bohdana Khmelnytskoho', '39', '3A', true, 0, NOW() - INTERVAL '300 days', NULL, 'seed', NULL),
    ('Viktoriia Kravchenko', 'viktoriia.kravchenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230009', 'CUSTOMER', 'Kyiv', 'Dorohozhytska', '88', NULL, true, 0, NOW() - INTERVAL '285 days', NULL, 'seed', NULL),
    ('Andrii Kovalchuk', 'andrii.kovalchuk@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230010', 'CUSTOMER', 'Kyiv', 'Nyzhnii Val', '65', '9', true, 0, NOW() - INTERVAL '270 days', NOW() - INTERVAL '9 days', 'seed', 'seed'),
    ('Kateryna Oliinyk', 'kateryna.oliinyk@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230011', 'CUSTOMER', 'Kyiv', 'Lypkivskogo', '19', NULL, true, 0, NOW() - INTERVAL '255 days', NULL, 'seed', NULL),
    ('Artem Savchenko', 'artem.savchenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230012', 'CUSTOMER', 'Kyiv', 'Yaroslaviv Val', '23', '5D', true, 0, NOW() - INTERVAL '240 days', NULL, 'seed', NULL),
    ('Yuliia Moroz', 'yuliia.moroz@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230013', 'CUSTOMER', 'Bucha', 'Heroiv Maidanu', '28', NULL, true, 0, NOW() - INTERVAL '225 days', NULL, 'seed', NULL),
    ('Bohdan Rudenko', 'bohdan.rudenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230014', 'CUSTOMER', 'Kyiv', 'Zhylianska', '46', '7A', true, 0, NOW() - INTERVAL '210 days', NULL, 'seed', NULL),
    ('Iryna Lysenko', 'iryna.lysenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230015', 'CUSTOMER', 'Kyiv', 'Solomianska', '52', NULL, true, 2, NOW() - INTERVAL '195 days', NOW() - INTERVAL '12 days', 'seed', 'seed'),
    ('Taras Marchenko', 'taras.marchenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230016', 'CUSTOMER', 'Kyiv', 'Hlybochytska', '93', '11B', true, 0, NOW() - INTERVAL '180 days', NULL, 'seed', NULL),
    ('Olena Hrytsenko', 'olena.hrytsenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230017', 'CUSTOMER', 'Irpin', 'Soborna', '17', NULL, true, 0, NOW() - INTERVAL '165 days', NULL, 'seed', NULL),
    ('Mykola Poliak', 'mykola.poliak@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230018', 'CUSTOMER', 'Kyiv', 'Prorizna', '44', '10', true, 0, NOW() - INTERVAL '150 days', NULL, 'seed', NULL),
    ('Alina Sydorenko', 'alina.sydorenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230019', 'CUSTOMER', 'Kyiv', 'Mezhyhirska', '26', NULL, true, 0, NOW() - INTERVAL '135 days', NULL, 'seed', NULL),
    ('Roman Ivanchuk', 'roman.ivanchuk@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230020', 'CUSTOMER', 'Kyiv', 'Saksahanskoho', '75', '2C', true, 0, NOW() - INTERVAL '120 days', NULL, 'seed', NULL),
    ('Mariia Kuzmenko', 'mariia.kuzmenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230021', 'CUSTOMER', 'Kyiv', 'Kyrylivska', '61', NULL, true, 0, NOW() - INTERVAL '105 days', NULL, 'seed', NULL),
    ('Denys Petrenko', 'denys.petrenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230022', 'CUSTOMER', 'Kyiv', 'Naberezhno-Khreshchatytska', '7', '15', false, 0, NOW() - INTERVAL '90 days', NULL, 'seed', NULL),
    ('Polina Zakharchenko', 'polina.zakharchenko@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230023', 'CUSTOMER', 'Bucha', 'Vokzalna', '81', NULL, true, 0, NOW() - INTERVAL '75 days', NULL, 'seed', NULL),
    ('Nazar Vovk', 'nazar.vovk@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230024', 'CUSTOMER', 'Kyiv', 'Dehtiarivska', '20', '4', false, 0, NOW() - INTERVAL '60 days', NULL, 'seed', NULL),
    ('Oksana Honchar', 'oksana.honchar@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230025', 'CUSTOMER', 'Kyiv', 'Turivska', '90', NULL, true, 0, NOW() - INTERVAL '45 days', NULL, 'seed', NULL),
    ('Yaroslav Danylchuk', 'yaroslav.danylchuk@example.com', '$2a$12$yyOs2pPT/SoBML2u99MKfewjp7Lhj1HrI/v5m7DFaC/SoMZetBJ0W', '+380501230026', 'CUSTOMER', 'Kyiv', 'Lukianivska', '35', '6', true, 0, NOW() - INTERVAL '30 days', NULL, 'seed', NULL),
    ('Ops Admin', 'ops.admin@example.com', '$2a$12$hPwjJfuUU7oG8KLBxWh23ulaVS6PyX2w7YvnJK8gIOyMYaHk24bfq', '+380501230027', 'ADMIN', 'Kyiv', 'Instytutska', '22', NULL, true, 0, NOW() - INTERVAL '200 days', NOW() - INTERVAL '3 days', 'seed', 'seed'),
    ('Menu Admin', 'menu.admin@example.com', '$2a$12$hPwjJfuUU7oG8KLBxWh23ulaVS6PyX2w7YvnJK8gIOyMYaHk24bfq', '+380501230028', 'ADMIN', 'Kyiv', 'Shota Rustaveli', '9', NULL, true, 0, NOW() - INTERVAL '160 days', NOW() - INTERVAL '4 days', 'seed', 'seed'),
    ('Courier Two', 'courier.two@example.com', '$2a$12$03vzvEtpaBM3Un4yjj0nweW9qMYVZrd2axRIatRRmTk5dLiL95bgW', '+380501230029', 'COURIER', 'Kyiv', 'Obolonska Embankment', '72', NULL, true, 0, NOW() - INTERVAL '140 days', NOW() - INTERVAL '1 day', 'seed', 'seed'),
    ('Courier Three', 'courier.three@example.com', '$2a$12$03vzvEtpaBM3Un4yjj0nweW9qMYVZrd2axRIatRRmTk5dLiL95bgW', '+380501230030', 'COURIER', 'Irpin', 'Tsentralna', '56', NULL, true, 0, NOW() - INTERVAL '100 days', NOW() - INTERVAL '1 day', 'seed', 'seed');

INSERT INTO products
    (slug, name, description, price, category, weight, pieces, available,
     created_at, updated_at, created_by, updated_by)
VALUES
    ('salmon-avocado-roll', 'Salmon Avocado Roll', 'Fresh salmon, avocado, cucumber and sesame.', 329.00, 'ROLL', 260, 8, true, NOW() - INTERVAL '300 days', NOW() - INTERVAL '5 days', 'admin@test.com', 'admin@test.com'),
    ('california-roll', 'California Roll', 'Crab mix, avocado, cucumber and tobiko.', 279.00, 'ROLL', 240, 8, true, NOW() - INTERVAL '295 days', NULL, 'admin@test.com', NULL),
    ('spicy-tuna-roll', 'Spicy Tuna Roll', 'Tuna, cucumber, scallion and house spicy sauce.', 309.00, 'ROLL', 230, 8, true, NOW() - INTERVAL '290 days', NULL, 'admin@test.com', NULL),
    ('philadelphia-roll', 'Philadelphia Roll', 'Salmon, cream cheese, avocado and cucumber.', 349.00, 'ROLL', 270, 8, true, NOW() - INTERVAL '285 days', NULL, 'admin@test.com', NULL),
    ('ebi-tempura-roll', 'Ebi Tempura Roll', 'Crispy shrimp, avocado, cucumber and teriyaki glaze.', 369.00, 'ROLL', 280, 8, true, NOW() - INTERVAL '280 days', NULL, 'admin@test.com', NULL),
    ('dragon-roll', 'Dragon Roll', 'Eel, shrimp tempura, avocado and unagi sauce.', 419.00, 'ROLL', 300, 8, false, NOW() - INTERVAL '275 days', NOW() - INTERVAL '1 day', 'admin@test.com', 'menu.admin@example.com'),
    ('vegan-garden-roll', 'Vegan Garden Roll', 'Avocado, cucumber, bell pepper, carrot and sesame.', 249.00, 'ROLL', 240, 8, true, NOW() - INTERVAL '270 days', NULL, 'admin@test.com', NULL),
    ('unagi-cucumber-roll', 'Unagi Cucumber Roll', 'Grilled eel, cucumber, sesame and unagi sauce.', 389.00, 'ROLL', 260, 8, true, NOW() - INTERVAL '265 days', NULL, 'admin@test.com', NULL),
    ('sakura-set', 'Sakura Set', 'A balanced 24-piece selection of salmon and classic rolls.', 899.00, 'SET', 720, 24, true, NOW() - INTERVAL '250 days', NULL, 'admin@test.com', NULL),
    ('tokyo-set', 'Tokyo Set', 'A 32-piece mix of maki, uramaki and nigiri.', 1199.00, 'SET', 960, 32, true, NOW() - INTERVAL '245 days', NULL, 'admin@test.com', NULL),
    ('salmon-lovers-set', 'Salmon Lovers Set', 'Twenty pieces of salmon nigiri, maki and uramaki.', 1049.00, 'SET', 620, 20, true, NOW() - INTERVAL '240 days', NULL, 'admin@test.com', NULL),
    ('family-party-set', 'Family Party Set', 'A generous 48-piece assortment made for sharing.', 1699.00, 'SET', 1480, 48, true, NOW() - INTERVAL '235 days', NULL, 'admin@test.com', NULL),
    ('matcha-latte', 'Matcha Latte', 'Ceremonial matcha with milk, lightly sweetened.', 119.00, 'DRINK', 400, NULL, true, NOW() - INTERVAL '220 days', NULL, 'admin@test.com', NULL),
    ('yuzu-soda', 'Yuzu Soda', 'Sparkling citrus drink with Japanese yuzu.', 99.00, 'DRINK', 330, NULL, true, NOW() - INTERVAL '215 days', NULL, 'admin@test.com', NULL),
    ('japanese-iced-tea', 'Japanese Iced Tea', 'Cold-brewed unsweetened jasmine green tea.', 89.00, 'DRINK', 500, NULL, true, NOW() - INTERVAL '210 days', NULL, 'admin@test.com', NULL),
    ('matcha-mochi', 'Matcha Mochi', 'Soft rice cakes with matcha cream filling.', 149.00, 'DESSERT', 150, 3, true, NOW() - INTERVAL '195 days', NULL, 'admin@test.com', NULL),
    ('mango-mochi', 'Mango Mochi', 'Soft rice cakes with mango cream filling.', 149.00, 'DESSERT', 150, 3, false, NOW() - INTERVAL '190 days', NOW() - INTERVAL '2 days', 'admin@test.com', 'menu.admin@example.com'),
    ('dorayaki-red-bean', 'Dorayaki Red Bean', 'Fluffy Japanese pancakes with sweet red bean paste.', 129.00, 'DESSERT', 120, 1, true, NOW() - INTERVAL '185 days', NULL, 'admin@test.com', NULL),
    ('miso-soup', 'Miso Soup', 'Miso broth with tofu, wakame and scallion.', 109.00, 'SOUP', 350, NULL, true, NOW() - INTERVAL '170 days', NULL, 'admin@test.com', NULL),
    ('spicy-ramen', 'Spicy Ramen', 'Chicken broth, noodles, egg, corn and chili oil.', 289.00, 'SOUP', 650, NULL, true, NOW() - INTERVAL '165 days', NULL, 'admin@test.com', NULL),
    ('seafood-udon-soup', 'Seafood Udon Soup', 'Udon noodles with shrimp, squid and vegetables.', 329.00, 'SOUP', 700, NULL, true, NOW() - INTERVAL '160 days', NULL, 'admin@test.com', NULL),
    ('wakame-salad', 'Wakame Salad', 'Seasoned seaweed with sesame and a light soy dressing.', 159.00, 'SALAD', 180, NULL, true, NOW() - INTERVAL '145 days', NULL, 'admin@test.com', NULL),
    ('salmon-avocado-salad', 'Salmon Avocado Salad', 'Salmon, avocado and greens with sesame dressing.', 299.00, 'SALAD', 320, NULL, true, NOW() - INTERVAL '140 days', NULL, 'admin@test.com', NULL),
    ('chicken-teriyaki-wok', 'Chicken Teriyaki Wok', 'Wok noodles, chicken, vegetables and teriyaki sauce.', 279.00, 'WOK', 450, NULL, true, NOW() - INTERVAL '125 days', NULL, 'admin@test.com', NULL),
    ('beef-yaki-udon', 'Beef Yaki Udon', 'Udon noodles with beef, vegetables and savory sauce.', 319.00, 'WOK', 480, NULL, true, NOW() - INTERVAL '120 days', NULL, 'admin@test.com', NULL),
    ('shrimp-yakisoba', 'Shrimp Yakisoba', 'Yakisoba noodles, shrimp, cabbage and carrot.', 329.00, 'WOK', 450, NULL, true, NOW() - INTERVAL '115 days', NULL, 'admin@test.com', NULL),
    ('vegetable-soba', 'Vegetable Soba', 'Buckwheat noodles with tofu and seasonal vegetables.', 259.00, 'WOK', 420, NULL, false, NOW() - INTERVAL '110 days', NOW() - INTERVAL '3 days', 'admin@test.com', 'menu.admin@example.com'),
    ('edamame', 'Edamame', 'Steamed young soybeans with sea salt.', 89.00, 'EXTRA', 150, NULL, true, NOW() - INTERVAL '95 days', NULL, 'admin@test.com', NULL),
    ('extra-ginger', 'Extra Ginger', 'A side portion of pickled sushi ginger.', 35.00, 'EXTRA', 50, NULL, false, NOW() - INTERVAL '90 days', NOW() - INTERVAL '4 days', 'admin@test.com', 'menu.admin@example.com'),
    ('spicy-mayo', 'Spicy Mayo', 'House mayonnaise blended with chili sauce.', 30.00, 'EXTRA', 40, NULL, true, NOW() - INTERVAL '85 days', NULL, 'admin@test.com', NULL);

-- Direct Unsplash CDN URLs are used so the storefront works without local upload files.
INSERT INTO product_images (product_id, url, sort_order)
SELECT p.id, image_data.url, 0
FROM (VALUES
    ('salmon-avocado-roll', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1200&q=80'),
    ('california-roll', 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=1200&q=80'),
    ('spicy-tuna-roll', 'https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=1200&q=80'),
    ('philadelphia-roll', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1200&q=80'),
    ('ebi-tempura-roll', 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=1200&q=80'),
    ('dragon-roll', 'https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=1200&q=80'),
    ('vegan-garden-roll', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1200&q=80'),
    ('unagi-cucumber-roll', 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=1200&q=80'),
    ('sakura-set', 'https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=1200&q=80'),
    ('tokyo-set', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1200&q=80'),
    ('salmon-lovers-set', 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=1200&q=80'),
    ('family-party-set', 'https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=1200&q=80'),
    ('matcha-latte', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=1200&q=80'),
    ('yuzu-soda', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=1200&q=80'),
    ('japanese-iced-tea', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=1200&q=80'),
    ('matcha-mochi', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=1200&q=80'),
    ('mango-mochi', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=1200&q=80'),
    ('dorayaki-red-bean', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=1200&q=80'),
    ('miso-soup', 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80'),
    ('spicy-ramen', 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80'),
    ('seafood-udon-soup', 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80'),
    ('wakame-salad', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80'),
    ('salmon-avocado-salad', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80'),
    ('chicken-teriyaki-wok', 'https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=1200&q=80'),
    ('beef-yaki-udon', 'https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=1200&q=80'),
    ('shrimp-yakisoba', 'https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=1200&q=80'),
    ('vegetable-soba', 'https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=1200&q=80'),
    ('edamame', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80'),
    ('extra-ginger', 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1200&q=80'),
    ('spicy-mayo', 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=1200&q=80')
) AS image_data(product_slug, url)
JOIN products p ON p.slug = image_data.product_slug;

INSERT INTO promotions
    (slug, title, description, discount_percent, start_date, end_date, active,
     created_at, updated_at, created_by, updated_by)
SELECT
    promotion_data.slug,
    promotion_data.title,
    promotion_data.description,
    promotion_data.discount_percent,
    CASE
        WHEN promotion_data.period_group = 'EXPIRED' THEN NOW() - INTERVAL '180 days'
        WHEN promotion_data.period_group = 'CURRENT_1Y' THEN NOW() - INTERVAL '14 days'
        WHEN promotion_data.period_group = 'CURRENT_2Y' THEN NOW() - INTERVAL '30 days'
        ELSE NOW() + INTERVAL '30 days'
    END,
    CASE
        WHEN promotion_data.period_group = 'EXPIRED' THEN NOW() - INTERVAL '30 days'
        WHEN promotion_data.period_group = 'CURRENT_1Y' THEN NOW() + INTERVAL '1 year'
        ELSE NOW() + INTERVAL '2 years'
    END,
    promotion_data.active,
    NOW() - (promotion_data.ordinal || ' days')::INTERVAL,
    NULL,
    'admin@test.com',
    NULL
FROM (VALUES
    (1, 'salmon-opening-offer', 'Salmon Opening Offer', 'Past campaign for the salmon avocado roll.', 10.00, 'EXPIRED', false),
    (2, 'california-week', 'California Week', 'Past campaign for a classic favourite.', 12.00, 'EXPIRED', false),
    (3, 'tuna-tuesday', 'Tuna Tuesday', 'Past spicy tuna promotion.', 15.00, 'EXPIRED', false),
    (4, 'philadelphia-days', 'Philadelphia Days', 'Past creamy roll campaign.', 10.00, 'EXPIRED', false),
    (5, 'tempura-crunch', 'Tempura Crunch', 'Past shrimp tempura campaign.', 18.00, 'EXPIRED', false),
    (6, 'dragon-special', 'Dragon Special', 'Past premium roll discount.', 20.00, 'EXPIRED', false),
    (7, 'green-choice', 'Green Choice', 'An expired campaign retained for history.', 8.00, 'EXPIRED', true),
    (8, 'unagi-festival', 'Unagi Festival', 'An expired eel roll campaign.', 15.00, 'EXPIRED', true),
    (9, 'sakura-launch', 'Sakura Launch', 'An expired set launch campaign.', 10.00, 'EXPIRED', true),
    (10, 'tokyo-night', 'Tokyo Night', 'An expired evening set campaign.', 12.00, 'EXPIRED', true),
    (11, 'salmon-fans', 'Salmon Fans', 'Long-running discount for salmon lovers.', 15.00, 'CURRENT_1Y', true),
    (12, 'family-table', 'Family Table', 'Long-running family set promotion.', 20.00, 'CURRENT_1Y', true),
    (13, 'matcha-moment', 'Matcha Moment', 'A year-long matcha drink offer.', 10.00, 'CURRENT_1Y', true),
    (14, 'yuzu-refresh', 'Yuzu Refresh', 'A refreshing yuzu soda discount.', 8.00, 'CURRENT_1Y', true),
    (15, 'iced-tea-daily', 'Iced Tea Daily', 'Everyday Japanese iced tea savings.', 5.00, 'CURRENT_1Y', true),
    (16, 'matcha-dessert', 'Matcha Dessert', 'Matcha mochi at a friendly price.', 12.00, 'CURRENT_1Y', true),
    (17, 'mango-sweet', 'Mango Sweet', 'Mango mochi promotion while stock lasts.', 10.00, 'CURRENT_1Y', true),
    (18, 'dorayaki-break', 'Dorayaki Break', 'A sweet afternoon dorayaki deal.', 8.00, 'CURRENT_1Y', true),
    (19, 'miso-comfort', 'Miso Comfort', 'Warm miso soup offer.', 6.00, 'CURRENT_1Y', true),
    (20, 'ramen-season', 'Ramen Season', 'A long seasonal spicy ramen campaign.', 14.00, 'CURRENT_1Y', true),
    (21, 'udon-long-weekend', 'Udon Long Weekend', 'Two-year seafood udon promotion.', 12.00, 'CURRENT_2Y', true),
    (22, 'wakame-wellness', 'Wakame Wellness', 'Two-year seaweed salad offer.', 10.00, 'CURRENT_2Y', true),
    (23, 'salad-balance', 'Salad Balance', 'Two-year salmon avocado salad offer.', 15.00, 'CURRENT_2Y', true),
    (24, 'teriyaki-favourite', 'Teriyaki Favourite', 'Two-year chicken wok campaign.', 12.00, 'CURRENT_2Y', true),
    (25, 'yaki-udon-club', 'Yaki Udon Club', 'Two-year beef udon campaign.', 10.00, 'CURRENT_2Y', true),
    (26, 'future-yakisoba', 'Future Yakisoba', 'Scheduled shrimp yakisoba campaign.', 18.00, 'FUTURE', true),
    (27, 'future-soba', 'Future Soba', 'Scheduled vegetable soba campaign.', 15.00, 'FUTURE', true),
    (28, 'future-edamame', 'Future Edamame', 'Scheduled edamame campaign.', 10.00, 'FUTURE', true),
    (29, 'ginger-backup', 'Ginger Backup', 'Prepared but disabled ginger campaign.', 5.00, 'FUTURE', false),
    (30, 'mayo-backup', 'Mayo Backup', 'Prepared but disabled spicy mayo campaign.', 5.00, 'FUTURE', false)
) AS promotion_data(ordinal, slug, title, description, discount_percent, period_group, active);

INSERT INTO promotion_products (promotion_id, product_id)
SELECT promotion.id, product.id
FROM generate_series(1, 30) AS seed_no
JOIN promotions promotion ON promotion.id = seed_no
JOIN products product ON product.id = seed_no;

WITH customer_ids AS (
    SELECT ARRAY_AGG(id ORDER BY id) AS ids
    FROM users
    WHERE user_role = 'CUSTOMER'
), order_seed AS (
    SELECT
        seed_no,
        CASE
            WHEN seed_no <= 24 THEN
                CASE ((seed_no - 1) % 7)
                    WHEN 0 THEN 'NEW'
                    WHEN 1 THEN 'CONFIRMED'
                    WHEN 2 THEN 'COOKING'
                    WHEN 3 THEN 'DELIVERING'
                    WHEN 4 THEN 'READY'
                    WHEN 5 THEN 'DELIVERED'
                    ELSE 'CANCELLED'
                END
            ELSE
                (ARRAY['CONFIRMED', 'COOKING', 'DELIVERING', 'READY', 'DELIVERED', 'CANCELLED'])[seed_no - 24]
        END AS status,
        CASE WHEN seed_no <= 24 THEN 'ONLINE' ELSE 'ON_DELIVERY' END AS payment_method,
        ((seed_no - 1) % 3) + 1 AS quantity
    FROM generate_series(1, 30) AS seed_no
), resolved AS (
    SELECT
        os.*,
        c.ids[((os.seed_no - 1) % CARDINALITY(c.ids)) + 1] AS user_id,
        CASE
            WHEN os.status = 'DELIVERING' THEN 'DELIVERY'
            WHEN os.status = 'READY' THEN 'PICKUP'
            WHEN os.seed_no % 3 = 0 THEN 'PICKUP'
            ELSE 'DELIVERY'
        END AS delivery_method
    FROM order_seed os
    CROSS JOIN customer_ids c
)
INSERT INTO orders
    (user_id, customer_name, phone, city, street, house, apartment, address_comment,
     status, payment_method, delivery_method, total_amount,
     created_at, updated_at, created_by, updated_by)
SELECT
    r.user_id,
    u.name,
    u.phone,
    CASE WHEN r.delivery_method = 'DELIVERY' THEN u.city ELSE NULL END,
    CASE WHEN r.delivery_method = 'DELIVERY' THEN u.street ELSE NULL END,
    CASE WHEN r.delivery_method = 'DELIVERY' THEN u.house ELSE NULL END,
    CASE WHEN r.delivery_method = 'DELIVERY' THEN u.apartment ELSE NULL END,
    CASE WHEN r.delivery_method = 'DELIVERY' THEN 'Please call on arrival' ELSE NULL END,
    r.status,
    r.payment_method,
    r.delivery_method,
    p.price * r.quantity,
    NOW() - ((31 - r.seed_no) || ' days')::INTERVAL,
    CASE WHEN r.status IN ('NEW', 'CONFIRMED') THEN NULL ELSE NOW() - ((30 - r.seed_no) || ' days')::INTERVAL END,
    u.email,
    CASE WHEN r.status IN ('NEW', 'CONFIRMED') THEN NULL ELSE 'admin@test.com' END
FROM resolved r
JOIN users u ON u.id = r.user_id
JOIN products p ON p.id = r.seed_no
ORDER BY r.seed_no;

INSERT INTO order_items (product_id, order_id, quantity, unit_price, subtotal)
SELECT
    p.id,
    o.id,
    ((o.id - 1) % 3) + 1,
    p.price,
    p.price * (((o.id - 1) % 3) + 1)
FROM orders o
JOIN products p ON p.id = o.id
ORDER BY o.id;

-- Twenty-four online orders have a primary payment. Six of them also have a failed retry.
INSERT INTO payments
    (stripe_session_id, order_id, amount, status, created_at, updated_at, created_by, updated_by)
SELECT
    'cs_seed_primary_' || LPAD(o.id::TEXT, 3, '0'),
    o.id,
    o.total_amount,
    CASE
        WHEN o.status = 'NEW' THEN 'PENDING'
        WHEN o.status = 'CANCELLED' THEN 'REFUNDED'
        ELSE 'PAID'
    END,
    o.created_at + INTERVAL '2 minutes',
    CASE WHEN o.status = 'NEW' THEN NULL ELSE o.created_at + INTERVAL '5 minutes' END,
    o.user_id::TEXT,
    CASE WHEN o.status = 'NEW' THEN NULL ELSE 'stripe-webhook' END
FROM orders o
WHERE o.payment_method = 'ONLINE';

INSERT INTO payments
    (stripe_session_id, order_id, amount, status, created_at, updated_at, created_by, updated_by)
SELECT
    'cs_seed_retry_' || LPAD(o.id::TEXT, 3, '0'),
    o.id,
    o.total_amount,
    'FAILED',
    o.created_at + INTERVAL '1 minute',
    o.created_at + INTERVAL '90 seconds',
    o.user_id::TEXT,
    'stripe-webhook'
FROM orders o
WHERE o.id IN (2, 4, 6, 8, 10, 12);

WITH customer_ids AS (
    SELECT ARRAY_AGG(id ORDER BY id) AS ids
    FROM users
    WHERE user_role = 'CUSTOMER'
), review_text AS (
    SELECT * FROM (VALUES
        (1, 5, 'Fresh salmon and a very balanced avocado ratio.'),
        (2, 4, 'Classic roll, neatly packed and still chilled.'),
        (3, 5, 'Good heat without covering the tuna flavour.'),
        (4, 4, 'Creamy and filling; the rice texture was right.'),
        (5, 5, 'The shrimp stayed crisp after delivery.'),
        (6, 4, 'Rich eel sauce and a generous portion.'),
        (7, 5, 'A colourful vegan option that does not feel like an extra.'),
        (8, 4, 'Smoky eel and crunchy cucumber worked well together.'),
        (9, 5, 'A convenient set for two people.'),
        (10, 4, 'Good variety and every piece arrived intact.'),
        (11, 5, 'Exactly what a salmon fan expects.'),
        (12, 5, 'Enough food for the whole group and nicely arranged.'),
        (13, 4, 'Smooth matcha taste and not overly sweet.'),
        (14, 4, 'Bright citrus flavour and plenty of fizz.'),
        (15, 3, 'Refreshing, though I would prefer a stronger tea aroma.'),
        (16, 5, 'Soft mochi with a pleasant matcha finish.'),
        (17, 4, 'The mango filling tastes natural.'),
        (18, 4, 'Soft pancakes and a comforting red bean filling.'),
        (19, 5, 'Warm, savoury and perfect beside a roll.'),
        (20, 5, 'Deep broth and a well judged spice level.'),
        (21, 4, 'The seafood was tender and the udon remained chewy.'),
        (22, 4, 'Crisp seaweed with a light sesame dressing.'),
        (23, 5, 'Fresh greens and a generous salmon portion.'),
        (24, 4, 'Reliable teriyaki flavour and plenty of vegetables.'),
        (25, 5, 'Tender beef and springy udon noodles.'),
        (26, 4, 'Shrimp were cooked well and the noodles were not oily.'),
        (27, 3, 'A light option; a little more sauce would be welcome.'),
        (28, 5, 'Simple, warm and properly salted.'),
        (29, 4, 'Crunchy ginger with enough acidity.'),
        (30, 4, 'Creamy sauce with a clean chili kick.')
    ) AS data(product_id, rating, comment)
)
INSERT INTO reviews
    (user_id, product_id, rating, comment, created_at, updated_at, created_by, updated_by)
SELECT
    c.ids[((rt.product_id - 1) % CARDINALITY(c.ids)) + 1],
    rt.product_id,
    rt.rating,
    rt.comment,
    NOW() - ((61 - rt.product_id) || ' days')::INTERVAL,
    CASE WHEN rt.product_id % 6 = 0 THEN NOW() - ((58 - rt.product_id) || ' days')::INTERVAL ELSE NULL END,
    'seed-customer',
    CASE WHEN rt.product_id % 6 = 0 THEN 'seed-customer' ELSE NULL END
FROM review_text rt
CROSS JOIN customer_ids c;

INSERT INTO review_replies
    (review_id, user_id, message, created_at, updated_at, created_by, updated_by)
SELECT
    r.id,
    (ARRAY[2, 27, 28])[((r.id - 1) % 3) + 1],
    CASE (r.id % 5)
        WHEN 0 THEN 'Thank you. We have shared your note with our kitchen team.'
        WHEN 1 THEN 'Thank you for the detailed review. We hope to serve you again soon.'
        WHEN 2 THEN 'We appreciate your feedback and are glad the order arrived well.'
        WHEN 3 THEN 'Thanks for choosing us. Your feedback helps us improve every order.'
        ELSE 'We are happy you enjoyed it. Thank you for taking the time to review.'
    END,
    r.created_at + INTERVAL '1 day',
    NULL,
    'admin@test.com',
    NULL
FROM reviews r
ORDER BY r.id;

WITH customer_ids AS (
    SELECT ARRAY_AGG(id ORDER BY id) AS ids
    FROM users
    WHERE user_role = 'CUSTOMER'
)
INSERT INTO tokens
    (token, token_type, user_id, used, expiry_date, created_at, updated_at, created_by, updated_by)
SELECT
    'seed-token-' || LPAD(seed_no::TEXT, 3, '0') || '-sushi-shop-demo',
    CASE WHEN seed_no % 2 = 0 THEN 'PASSWORD_RESET' ELSE 'EMAIL_VERIFICATION' END,
    c.ids[((seed_no - 1) % CARDINALITY(c.ids)) + 1],
    CASE WHEN seed_no <= 10 THEN true ELSE false END,
    CASE
        WHEN seed_no <= 10 THEN NOW() - (seed_no || ' days')::INTERVAL
        WHEN seed_no <= 15 THEN NOW() + (seed_no || ' days')::INTERVAL
        WHEN seed_no <= 20 THEN NOW() + INTERVAL '90 days'
        WHEN seed_no <= 25 THEN NOW() + INTERVAL '1 year'
        ELSE NOW() + INTERVAL '2 years'
    END,
    NOW() - ((31 - seed_no) || ' days')::INTERVAL,
    CASE WHEN seed_no <= 10 THEN NOW() - ((seed_no - 1) || ' days')::INTERVAL ELSE NULL END,
    'seed',
    CASE WHEN seed_no <= 10 THEN 'seed' ELSE NULL END
FROM generate_series(1, 30) AS seed_no
CROSS JOIN customer_ids c;

INSERT INTO audit_logs (action, entity_name, entity_id, details, performed_by, performed_at)
SELECT
    (ARRAY['CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'EXPORT'])[((seed_no - 1) % 6) + 1],
    (ARRAY['Product', 'Promotion', 'Order', 'User', 'Review'])[((seed_no - 1) % 5) + 1],
    CASE WHEN seed_no % 6 IN (3, 4, 5) THEN NULL ELSE ((seed_no - 1) % 30) + 1 END,
    CASE (seed_no % 4)
        WHEN 0 THEN 'Seeded status change for dashboard testing'
        WHEN 1 THEN 'Seeded entity operation with valid test data'
        WHEN 2 THEN 'Seeded authentication activity'
        ELSE 'Seeded export or cleanup activity'
    END,
    (ARRAY['admin@test.com', 'ops.admin@example.com', 'menu.admin@example.com'])[((seed_no - 1) % 3) + 1],
    NOW() - (seed_no || ' hours')::INTERVAL
FROM generate_series(1, 30) AS seed_no;

DO $$
DECLARE
    table_name TEXT;
    actual_count BIGINT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'users', 'products', 'product_images', 'promotions', 'promotion_products',
        'orders', 'order_items', 'payments', 'reviews', 'review_replies',
        'tokens', 'audit_logs'
    ]
    LOOP
        EXECUTE FORMAT('SELECT COUNT(*) FROM %I', table_name) INTO actual_count;
        IF actual_count <> 30 THEN
            RAISE EXCEPTION 'Seed validation failed: table % has % rows instead of 30', table_name, actual_count;
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1
        FROM orders o
        JOIN (
            SELECT order_id, SUM(subtotal) AS item_total
            FROM order_items
            GROUP BY order_id
        ) totals ON totals.order_id = o.id
        WHERE o.total_amount <> totals.item_total
    ) THEN
        RAISE EXCEPTION 'Seed validation failed: an order total does not match its item subtotal';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM payments p
        JOIN orders o ON o.id = p.order_id
        WHERE p.amount <> o.total_amount
    ) THEN
        RAISE EXCEPTION 'Seed validation failed: a payment amount does not match its order total';
    END IF;

    IF EXISTS (
        SELECT product_id
        FROM promotion_products pp
        JOIN promotions p ON p.id = pp.promotion_id
        WHERE p.active = true AND p.start_date <= NOW() AND p.end_date >= NOW()
        GROUP BY product_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Seed validation failed: a product belongs to overlapping active promotions';
    END IF;
END
$$;

COMMIT;

SELECT table_name, row_count
FROM (VALUES
    ('users', (SELECT COUNT(*) FROM users)),
    ('products', (SELECT COUNT(*) FROM products)),
    ('product_images', (SELECT COUNT(*) FROM product_images)),
    ('promotions', (SELECT COUNT(*) FROM promotions)),
    ('promotion_products', (SELECT COUNT(*) FROM promotion_products)),
    ('orders', (SELECT COUNT(*) FROM orders)),
    ('order_items', (SELECT COUNT(*) FROM order_items)),
    ('payments', (SELECT COUNT(*) FROM payments)),
    ('reviews', (SELECT COUNT(*) FROM reviews)),
    ('review_replies', (SELECT COUNT(*) FROM review_replies)),
    ('tokens', (SELECT COUNT(*) FROM tokens)),
    ('audit_logs', (SELECT COUNT(*) FROM audit_logs))
) AS seeded(table_name, row_count)
ORDER BY table_name;
