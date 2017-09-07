<?php
include('authentication.php');

// $_POST['name'] contains the name of the whole rent
if (!isset($_POST['name'])) {
  exit();
}

/* Set prices data are given
  $_POST['prices'] contains a set of keys/values in braces.
  Each key/value is seperated by ', ' and  each key is seperated by '=' from its value. 
  $_POST['subrent'] contains the rent to set prices. */
if (isset($_POST['prices']) && isset($_POST['subrent']) && isset($_POST['year'])) {
  /* Check if the user has the right to modify prices */
  $req = $bdd->prepare('SELECT DISTINCT o.user FROM owner o, subrent s, rent r
                        WHERE r.name = :name AND (
                            o.rent = r.id 
                            OR 
                            (o.rent = s.subrent AND s.rent = r.id));');
  $req->execute(array('name' => $_POST['name']));
  $owners = [];
  while ($res = $req->fetch())
    $owners[] = $res['user'];
  $req->closeCursor();

  if ($user['access'] <= 1 && !in_array($user['id'], $owners)) {
    echo "No right to edit prices";
    exit();
  }
  
  /* Insert, update or remove prices */
  $values_str = '';
  $values_array = array('rent' => $_POST['subrent'], 
                        'year' => $_POST['year']);
  $week_to_remove = [];
  $i = 0;
  // Remove braces
  $prices_str = substr($_POST['prices'], 1, -1);
  // Browse the list of keys/values
  foreach (explode(', ', $prices_str) as $week_price) {
    [$week, $price] = explode('=', $week_price);
    if ($price >= 0) {
      $values_str .= '(:rent, :year, :week'.$i.', :price'.$i.'),';
      $values_array['week'.$i] = $week;
      $values_array['price'.$i] = $price;
      $i += 1;
    }
    else {
      // If the price is negative, remove the week price from the database
      $week_to_remove[] = intval($week);
    }
  }
  if (count($values_str) > 0) {
    // Remove the last coma 
    $values_str = substr($values_str, 0, -1);
    // Insert or update prices
    $req = $bdd->prepare('INSERT INTO price (rent, year, week, price)
                          VALUES '.$values_str.'
                          ON DUPLICATE KEY UPDATE price=VALUES(price);');
    $req->execute($values_array);
    $req->closeCursor();
  }
  if (count($week_to_remove) > 0) {
    // Delete week prices
    $req = $bdd->prepare('DELETE FROM price
                          WHERE rent = :rent 
                          AND week IN ('.join(',',$week_to_remove).');');
    $req->execute(array('rent' => $_POST['subrent']));
    $req->closeCursor();
  }
}

// Select subrents of the given rent (and the rent itself)
$req = $bdd->prepare('SELECT * FROM rent
                      WHERE id IN (
                          SELECT s.subrent FROM subrent s, rent r
                          WHERE s.rent = r.id AND r.name = :name
                      ) OR name = :name
                      ORDER BY id;');
$req->execute(array('name' => $_POST['name']));
$subrents_id = [];
$subrents_names = [];
$idRent = -1;
while ($rent = $req->fetch()) {
  $subrents_id[] = $rent['id'];
  $subrents_names[] = $rent['name'];
  if ($_POST['name'] == $rent['name'])
    $idRent = $rent['id'];
}
$req->closeCursor();
// If idRent == -1, the rent name is invalid
if ($idRent == -1) {
  echo "Invalid rent name";
  exit();
}

// Select prices for the following years
$curr_year = date("Y");
$selected_years = [$curr_year-1, $curr_year, $curr_year+1];

$prices = [];
// Organize prices in rent
foreach ($subrents_id as $id) {
  $prices[$id] = [];
  // Organize prices in year
  foreach ($selected_years as $year) {
    $prices[$id][$year] = [];
  }
}

// Recover information concerning prices
$req = $bdd->prepare('SELECT * FROM price
                      WHERE rent IN ('.join(",",$subrents_id).')
                      AND year IN ('.join(",",$selected_years).')
                      ORDER BY year DESC;');
$req->execute();
while ($res = $req->fetch()) {
  $year = $res['year'];
  $rent_id = $res['rent'];
  $week = $res['week'];
  $price = $res['price'];
  $prices[$rent_id][$year][] = array($week => $price);
}
$req->closeCursor();

$subrents = [];
for ($i = 0; $i < count($subrents_id); $i++) {
  $rent_id = $subrents_id[$i];
  $rent_name = $subrents_names[$i];
  $year = $subrents_years[$rent_id];
  $subrents[] = array('id' => $rent_id,
                      'name' => $rent_name,
                      'prices' => $prices[$rent_id]);
}
  
echo json_encode(array('username' => $user['username'],
                       'hash' => $user['password'],
                       'subrents' => $subrents));
?>