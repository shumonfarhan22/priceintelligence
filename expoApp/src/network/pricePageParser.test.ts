import { describe, expect, it } from 'vitest';

import { isValidProductImageUrl, parseRetailerPage } from './pricePageParser';

describe('retailer page parser', () => {
  it('validates product image URLs and rejects UI badges/icons', () => {
    // Valid Amazon product images (/images/I/)
    expect(isValidProductImageUrl('https://m.media-amazon.com/images/I/41HYxmKrAWL._SX679_.jpg', 'https://amazon.in')).toBe(true);
    expect(isValidProductImageUrl('https://images-eu.ssl-images-amazon.com/images/I/51o6S0iXNCL._AC_UL165_SR165,165_.jpg')).toBe(true);

    // Invalid Amazon graphics/badges (/images/G/, prime, badge, logo)
    expect(isValidProductImageUrl('https://m.media-amazon.com/images/G/31/marketing/prime/Prime_Logo.png')).toBe(false);
    expect(isValidProductImageUrl('https://images-eu.ssl-images-amazon.com/images/G/31/prime/badge.png')).toBe(false);
    expect(isValidProductImageUrl('https://m.media-amazon.com/images/I/logo_test.jpg')).toBe(false);
    expect(isValidProductImageUrl('https://m.media-amazon.com/images/I/transparent.png')).toBe(false);

    // Valid Flipkart product images
    expect(isValidProductImageUrl('https://rukminim1.flixcart.com/image/1500/1500/xif0q/pot/cooker.jpeg', 'https://flipkart.com')).toBe(true);
    // Invalid Flipkart badges
    expect(isValidProductImageUrl('https://static-assets-web.flixcart.com/fk-p-linchpin-web/fk-cp-zion/img/plus-badge.png', 'https://flipkart.com')).toBe(false);
  });
  it('reads an Amazon product from a JSON-LD graph', () => {
    const html = `
      <html><head><script type="application/ld+json">
      {"@graph":[{"@type":"Product","image":["https://images.example/phone.jpg"],
      "offers":{"@type":"Offer","price":"12,499.00"}}]}
      </script></head></html>`;

    expect(parseRetailerPage(html, 'https://www.amazon.in/example')).toEqual({
      price: 12_499,
      image: 'https://images.example/phone.jpg',
      blocked: false,
    });
  });

  it('reads offer arrays, price specifications, and object images', () => {
    const html = `<script type='application/ld+json'>
      {"@type":["Thing","Product"],"image":{"contentUrl":"https://images.example/item.png"},
      "offers":[{"priceSpecification":{"price":"₹899"}}]}
    </script>`;

    expect(parseRetailerPage(html, 'https://www.flipkart.com/example')).toMatchObject({
      price: 899,
      image: 'https://images.example/item.png',
    });
  });

  it('uses Amazon HTML and dynamic-image fallbacks', () => {
    const html = `
      <img id="landingImage" src="/small.jpg"
        data-a-dynamic-image='{&quot;https://images.example/large.jpg&quot;:[1200,1200]}' />
      <span class="a-price"><span class="a-offscreen">₹1,299.50</span></span>`;

    expect(parseRetailerPage(html, 'https://www.amazon.in/product/1')).toMatchObject({
      price: 1_299.5,
      image: 'https://images.example/large.jpg',
    });
  });

  it('uses Flipkart and metadata fallbacks', () => {
    const flipkart = parseRetailerPage(
      '<img class="DByuf4" src="https://images.example/flipkart.jpg"><div class="Nx9bqj">₹2,599</div>',
      'https://www.flipkart.com/product/1',
    );
    expect(flipkart).toMatchObject({ price: 2_599, image: 'https://images.example/flipkart.jpg' });

    const flipkartEmbedded = parseRetailerPage(
      '<script>window.__INITIAL_STATE__ = {"pageDataV4":{"page":{"data":{"1000":{"data":{"finalPrice":1845}}}}}};</script><meta property="og:image" content="https://rukminim2.flixcart.com/image/1500/1500/xif0q/pot/m/8/n/no-1-cl15-hawkins-enriched-transparent-original-imah5g8trzwwemhy.png">',
      'https://www.flipkart.com/hawkins-pressure-cooker/p/itm123',
    );
    expect(flipkartEmbedded).toMatchObject({
      price: 1845,
      image: 'https://rukminim2.flixcart.com/image/500/500/xif0q/pot/m/8/n/no-1-cl15-hawkins-enriched-transparent-original-imah5g8trzwwemhy.png',
    });

    const flipkartFsp = parseRetailerPage(
      '<script>var obj = {"price":{"fsp":799}};</script><img class="_396cs4" src="https://rukminim2.flixcart.com/image/832/832/xif0q/shirt/test-original-123.jpeg" />',
      'https://www.flipkart.com/shirt/p/itm456',
    );
    expect(flipkartFsp).toMatchObject({
      price: 799,
      image: 'https://rukminim2.flixcart.com/image/500/500/xif0q/shirt/test-original-123.jpeg',
    });

    const metadata = parseRetailerPage(
      '<meta property="product:price:amount" content="4,499.00"><meta property="og:image" content="/meta.jpg">',
      'https://www.amazon.in/product/2',
    );
    expect(metadata).toMatchObject({ price: 4_499, image: 'https://www.amazon.in/meta.jpg' });
  });

  it('flags short challenge pages without misreading normal pages', () => {
    expect(parseRetailerPage(
      '<html><head><title>Robot Check</title></head><body>Enter the characters you see below</body></html>',
      'https://amazon.in/product',
    ).blocked).toBe(true);

    const ordinary = `<html><body>${'This robot vacuum cleaner is useful. '.repeat(40)}</body></html>`;
    expect(parseRetailerPage(ordinary, 'https://amazon.in/product').blocked).toBe(false);
  });

  it('strictly rejects Amazon Prime badges and extracts the real product photo in medium quality', () => {
    const html = `
      <div>
        <img alt="Amazon Prime Logo" src="https://m.media-amazon.com/images/G/31/marketing/prime/2022PrimeBrand/Logos/Prime_Logo_RGB_Prime_Blue_MASTER._CB542734830_.png" class="a-dynamic-image" />
        <div id="corePriceDisplay_desktop_feature_div">
          <span class="a-price a-text-price" data-a-strike="true"><span class="a-offscreen">₹3,670.00</span></span>
          <span class="priceToPay"><span class="a-price"><span class="a-offscreen">₹3,065.00</span></span></span>
        </div>
        <img id="landingImage" src="https://m.media-amazon.com/images/I/41HYxmKrAWL._SY355_.jpg"
          data-a-dynamic-image='{"https://m.media-amazon.com/images/I/41HYxmKrAWL._SX425_.jpg":[425,425],"https://m.media-amazon.com/images/I/41HYxmKrAWL._SX679_.jpg":[679,679]}' />
      </div>
    `;

    const parsed = parseRetailerPage(html, 'https://www.amazon.in/dp/B09F3M8P4K');
    expect(parsed.price).toBe(3065);
    expect(parsed.image).toBe('https://m.media-amazon.com/images/I/41HYxmKrAWL._SL500_.jpg');
    expect(parsed.image).not.toContain('/images/G/');
    expect(parsed.image).not.toContain('prime');
  });

  it('rejects general /images/G/ assets, UI icons, and ratings from Amazon and normalizes to medium quality', () => {
    const html = `
      <img src="https://m.media-amazon.com/images/G/31/icon.png" />
      <span class="a-icon-alt">4.2 out of 5 stars</span>
      <div id="imgTagWrapperId">
        <img src="https://m.media-amazon.com/images/I/71xyz123.jpg" />
      </div>
      <span class="a-price"><span class="a-offscreen">₹1,499</span></span>
    `;
    const parsed = parseRetailerPage(html, 'https://www.amazon.in/dp/B012345678');
    expect(parsed.image).toBe('https://m.media-amazon.com/images/I/71xyz123._SL500_.jpg');
    expect(parsed.price).toBe(1499);
  });

  it('normalizes Flipkart and Amazon URLs to balanced medium quality (~500px)', () => {
    // Flipkart thumbnail -> medium
    expect(parseRetailerPage(
      '<img class="_396cs4" src="https://rukmini1.flixcart.com/image/128/128/xif0q/pot/cooker.jpeg" /><span class="_30jeq3">₹999</span>',
      'https://flipkart.com/cooker/p/itm1',
    ).image).toBe('https://rukminim2.flixcart.com/image/500/500/xif0q/pot/cooker.jpeg');

    // Flipkart high-res -> medium
    expect(parseRetailerPage(
      '<img class="_396cs4" src="https://rukminim2.flixcart.com/image/1500/1500/xif0q/pot/cooker.jpeg" /><span class="_30jeq3">₹999</span>',
      'https://flipkart.com/cooker/p/itm1',
    ).image).toBe('https://rukminim2.flixcart.com/image/500/500/xif0q/pot/cooker.jpeg');

    // Amazon low-res thumbnail -> medium
    expect(parseRetailerPage(
      '<div id="imgTagWrapperId"><img src="https://m.media-amazon.com/images/I/61abc._SL75_.jpg" /></div><span class="a-price"><span class="a-offscreen">₹999</span></span>',
      'https://amazon.in/dp/B1',
    ).image).toBe('https://m.media-amazon.com/images/I/61abc._SL500_.jpg');

    // Amazon high-res -> medium
    expect(parseRetailerPage(
      '<div id="imgTagWrapperId"><img src="https://m.media-amazon.com/images/I/61abc._SL1500_.jpg" /></div><span class="a-price"><span class="a-offscreen">₹999</span></span>',
      'https://amazon.in/dp/B1',
    ).image).toBe('https://m.media-amazon.com/images/I/61abc._SL500_.jpg');
  });

  it('returns a safe empty result for blank or unrecognized pages', () => {
    expect(parseRetailerPage('', 'https://amazon.in/product')).toEqual({
      price: null,
      image: null,
      blocked: false,
    });
    expect(parseRetailerPage('<body>Unavailable</body>', 'https://amazon.in/product').price).toBeNull();
  });
});
