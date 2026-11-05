import {
  Alert,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { useAuth } from '@/contexts/AuthContext';

export default function Settings() {
  const { signOut } = useAuth();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Settings</Text>
      <Text style={styles.subtitle}>Manage your account.</Text>

      <Text style={styles.section}>ACCOUNT</Text>

      <Pressable style={styles.item}>
        <Text style={styles.itemText}>Personal Information</Text>
        <Text style={styles.arrow}>›</Text>
      </Pressable>

      <Pressable style={styles.item}>
        <Text style={styles.itemText}>Change Password</Text>
        <Text style={styles.arrow}>›</Text>
      </Pressable>

      <Text style={styles.section}>PREFERENCES</Text>

      <Pressable style={styles.item}>
        <Text style={styles.itemText}>Notifications</Text>
        <Text style={styles.arrow}>›</Text>
      </Pressable>

      <Pressable style={styles.item}>
        <Text style={styles.itemText}>About Rydr</Text>
        <Text style={styles.arrow}>›</Text>
      </Pressable>

      <Text style={styles.section}>SESSION</Text>

      <Pressable
        style={styles.logout}
        onPress={() => {
          Alert.alert(
            'Log out',
            'Are you sure you want to log out?',
            [
              { text: 'Cancel', style: 'cancel' },
              {
                text: 'Log out',
                style: 'destructive',
                onPress: signOut,
              },
            ]
          );
        }}
      >
        <Text style={styles.logoutText}>Log out</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    padding: 24,
    paddingTop: 60,
  },

  title: {
    fontSize: 32,
    fontWeight: 'bold',
    marginBottom: 8,
  },

  subtitle: {
    fontSize: 16,
    color: '#777',
    marginBottom: 40,
  },

  section: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#888',
    marginBottom: 12,
    marginTop: 24,
  },

  item: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 18,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },

  itemText: {
    fontSize: 16,
  },

  arrow: {
    fontSize: 22,
    color: '#999',
  },

  logout: {
    paddingVertical: 18,
  },

  logoutText: {
    color: '#e53935',
    fontSize: 16,
    fontWeight: '600',
  },
});