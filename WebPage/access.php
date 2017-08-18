<?php
if(!isset($_POST['login']) || 
    (!isset($_POST['password']) && !isset($_POST['hash']))) {
  echo "Access denied !";
  exit();
}
include('./bdd.php');

$bdd_user = $bdd->prepare('SELECT * FROM user WHERE login = :login AND password = :password');
if (isset($_POST['password'])) {
  $password = hash("sha512", $_POST['password'], false);
}
else {
  $password = $_POST['hash'];
}
$bdd_user->execute(array('login' => $_POST['login'],
                         'password' => $password));
$user = $bdd_user->fetchAll();
$bdd_user->closeCursor();

if (isset($user[0])) {
  $bdd_rents = $bdd->prepare('SELECT * FROM rent');
  $bdd_rents->execute(array('id_user' => $user[0]['id']));
  $rents = $bdd_rents->fetchAll();
  $bdd_rents->closeCursor();
  
  echo json_encode(array('login' => $user[0]['login'],
                         'password' => $user[0]['password'],
                         'rents' => $rents));
}
else {
  echo "{}";
  exit();
}

?>