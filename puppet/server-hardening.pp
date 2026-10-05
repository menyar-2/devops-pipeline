# ==========================================
# SIMP-LIKE SERVER HARDENING - ROCKY LINUX
# ==========================================

exec { 'disable_ipv4_forwarding':
  command => '/usr/sbin/sysctl -w net.ipv4.ip_forward=0',
  unless  => '/bin/test "$(/usr/sbin/sysctl -n net.ipv4.ip_forward)" = "0"',
}

exec { 'disable_accept_redirects':
  command => '/usr/sbin/sysctl -w net.ipv4.conf.all.accept_redirects=0',
  unless  => '/bin/test "$(/usr/sbin/sysctl -n net.ipv4.conf.all.accept_redirects)" = "0"',
}

exec { 'disable_send_redirects':
  command => '/usr/sbin/sysctl -w net.ipv4.conf.all.send_redirects=0',
  unless  => '/bin/test "$(/usr/sbin/sysctl -n net.ipv4.conf.all.send_redirects)" = "0"',
}

file { '/etc/passwd':
  owner => 'root',
  group => 'root',
  mode  => '0644',
}

file { '/etc/shadow':
  owner => 'root',
  group => 'root',
  mode  => '0000',
}

file { '/etc/group':
  owner => 'root',
  group => 'root',
  mode  => '0644',
}

file { '/etc/sysctl.d/99-devsecops-hardening.conf':
  ensure  => file,
  owner   => 'root',
  group   => 'root',
  mode    => '0644',
  content => "net.ipv4.ip_forward = 0\nnet.ipv4.conf.all.accept_redirects = 0\nnet.ipv4.conf.all.send_redirects = 0\n",
}

exec { 'reload_sysctl_hardening':
  command     => '/usr/sbin/sysctl --system',
  refreshonly => true,
  subscribe   => File['/etc/sysctl.d/99-devsecops-hardening.conf'],
}
