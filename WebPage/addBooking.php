<?php
include('authentication.php');

if (!isset($_POST['rent']) || !isset($_POST['tenant']) || 
    !isset($_POST['week']) || !isset($_POST['year'])) {
  exit();
}

// Check if the id is in the table of rents and if the table is a sub-rent or not.
$req = $bdd->prepare('SELECT subrent, COUNT(rent) as nb FROM (
                          SELECT rent, subrent FROM subrent
                          WHERE subrent = :rent
                          UNION
                          SELECT id as rent, id as subrent FROM rent
                          WHERE id = :rent
                      ) as temp
                      GROUP BY subrent;');
$req->execute(array('rent' => $_POST['rent']));
$result = $req->fetch();
$req->closeCursor();

$isSubrent = false;
if ($result) {
  // Check if the rent id is a sub-rent or the whole rent
  if ($result['nb'] > 1)
    $isSubrent = true;
}
else {
  // The given rent id is unknown
  exit();
}

// Check if the user has the right to add a booking
$req = $bdd->prepare('SELECT DISTINCT o.user FROM owner o, subrent s
                      WHERE o.rent = :rent
                      OR (o.rent = s.rent AND s.subrent = :rent);');
$req->execute(array('rent' => $_POST['rent']));
$owners = [];
while ($res = $req->fetch())
  $owners[] = $res['user'];
$req->closeCursor();

if ($user['access'] == 0 && !in_array($user['id'], $owners)) {
  echo "No booking right";
  exit();
}

$req = null;
if ($isSubrent) {
  /* Insert into booking by checking if the whole rent is rented at the given date.
     If this is the case, no row is inserted. */
  $req = $bdd->prepare('INSERT INTO booking (rent, week, year, tenant)
                        SELECT :rent, :week, :year, :tenant
                        FROM dual
                        WHERE NOT EXISTS (
                            SELECT 1 FROM subrent s, booking b
                            WHERE b.rent = s.rent AND s.subrent = :rent
                                  AND b.week = :week AND b.year = :year
                        );');
}
else {
  /* Insert into booking by checking if a sub-rent is rented.
     If this is the case, no row is inserted. */
  $req = $bdd->prepare('INSERT INTO booking (rent, week, year, tenant)
                        SELECT :rent, :week, :year, :tenant
                        FROM dual
                        WHERE NOT EXISTS (
                            SELECT 1 FROM subrent s, booking b
                            WHERE b.rent = s.subrent AND s.rent = :rent
                                  AND b.week = :week AND b.year = :year
                        );');
}
$success = $req->execute(array('rent' => $_POST['rent'],
                               'week' => $_POST['week'],
                               'year' => $_POST['year'],
                               'tenant' => $_POST['tenant']));
$rowsInserted = $req->rowCount();
$req->closeCursor();

// (rent, week, year) are unique in the table.
// So if a booking already exists for a rent at the given date,
// no row is inserted and execute() return false (true otherwise).
if ($success && $rowsInserted == 1)
  echo "true";
else
  echo "Rent not free";

?>