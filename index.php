<?php
require 'vendor/autoload.php';
// if(isset($_POST['authKey']) && ($_POST['authKey']) == 'abc') {}
$stripe = new \Stripe\StripeClient('sk_test_51N2SLaEnplNhvS03hpmPc7W7SCaCTuWzBOtFljRiRBjysj9hgzXVNakuubMPO1WjvewWXjG4PgSmUcR4p36jJuKO00LPybPQLf');

// Use an existing Customer ID if this is a returning customer.
$customer = $stripe->customers->create([
    'name' => "John Doe",
    'address' => [
        'line1' => 'Demo address',
        'postal_code' => '63000',
        'city' => 'Cyberjaya',
        'state' => 'Selangor',
        'country' => 'Malaysia',
    ]
]);
$ephemeralKey = $stripe->ephemeralKeys->create([
  'customer' => $customer->id,
], [
  'stripe_version' => '2022-08-01',
]);
$paymentIntent = $stripe->paymentIntents->create([
  'amount' => '', // 10.99 myr
  'currency' => 'myr',
  'description' => 'Payment for Android Course',
  'customer' => $customer->id,
  'automatic_payment_methods' => [
    'enabled' => 'true',
  ],
]);

echo json_encode(
  [
    'paymentIntent' => $paymentIntent->client_secret,
    'ephemeralKey' => $ephemeralKey->secret,
    'customer' => $customer->id,
    'publishableKey' => 'pk_test_51N2SLaEnplNhvS03m1LwOOMMTwerNmGkdHsnCsbneFtiNyC9hJYQ3hNgoB3VaymIvxTJNaW7w0saKvIdWAGG6Z56007eluG6G7'
  ]
);
http_response_code(200);