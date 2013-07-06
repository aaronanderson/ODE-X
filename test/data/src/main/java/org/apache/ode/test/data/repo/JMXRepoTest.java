package org.apache.ode.test.data.repo;


public class JMXRepoTest {
	/*
	protected static Repository jmxRepo;
	protected static org.apache.ode.spi.repo.Repository repo;
	
	@BeforeClass
public static void setUpBeforeClass() throws Exception {
repo = (org.apache.ode.spi.repo.Repository) container.getBeanManager().getReference(repoBean, org.apache.ode.spi.repo.Repository.class, repoCtx);
jmxRepo = (Repository) container.getBeanManager().getReference(jmxRepoBean, Repository.class, jmxRepoCtx);

}

@AfterClass
public static void tearDownAfterClass() throws Exception {

}


	@Test
	public void testRepo() throws Exception {
		assertNotNull(repo);
		repo.registerFileExtension("bar", "application/bar");
		assertNotNull(jmxRepo);
		ArtifactId id = new ArtifactId("{http://bar.com/bar}", "application/bar", "1.0");
		id = jmxRepo.importArtifact(id, "foo.bar", false, false, "this is some bar".getBytes());
		assertNotNull(id);
		assertEquals(id.getName(), "{http://bar.com/bar}");
		assertEquals(id.getVersion(), "1.0");
		assertEquals(id.getType(), "application/bar");
		jmxRepo.refreshArtifact(id, false, "this is some foo bar".getBytes());
		byte[] contents = jmxRepo.exportArtifact(id);
		assertNotNull(contents);
		assertEquals("this is some foo bar", new String(contents));
		jmxRepo.removeArtifact(id);
		assertFalse(repo.exists(QName.valueOf(id.getName()), id.getType(), id.getVersion()));
	}
*/
}
