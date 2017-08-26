<?php
include('authentication.php');
  
echo json_encode(array('login' => $user['login'],
                       'hash' => $user['password']));
?>