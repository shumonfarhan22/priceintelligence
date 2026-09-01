import { describe, expect, it } from 'vitest';

import { parseRetailerPage } from './pricePageParser';

describe('retailer page parser', () => {
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

  it('returns a safe empty result for blank or unrecognized pages', () => {
    expect(parseRetailerPage('', 'https://amazon.in/product')).toEqual({
      price: null,
      image: null,
      blocked: false,
    });
    expect(parseRetailerPage('<body>Unavailable</body>', 'https://amazon.in/product').price).toBeNull();
  });
});
