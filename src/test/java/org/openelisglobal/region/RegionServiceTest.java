package org.openelisglobal.region;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.region.service.RegionService;
import org.openelisglobal.region.valueholder.Region;
import org.springframework.beans.factory.annotation.Autowired;

public class RegionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    RegionService regionService;

    private Map<String, String> testRegions;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/region.xml");
    }

    @After
    public void tearDown() {
        List<Region> regions = regionService.getAll();
        for (Region region : regions) {
            if (region.getLastupdated() == null) {
                region.setLastupdated(new Timestamp(System.currentTimeMillis()));
            }
        }
        regionService.deleteAll(regions);

    }

    @Test
    public void createRegion_shouldCreateNewRegion() throws Exception {
        String regionName = "Midwest";
        String regionId = "1";
        Region region = createRegion(regionName, regionId);

        String savedRegionId = regionService.insert(region);

        Region savedRegion = regionService.get(savedRegionId);
        Assert.assertEquals(regionName, savedRegion.getRegion());
        Assert.assertNotNull(savedRegion.getId());
    }

    @Test
    public void updateRegion_shouldUpdateExistingRegion() throws Exception {
        Region existingRegion = regionService.get("2");
        Assert.assertNotNull(existingRegion);

        existingRegion.setRegion("Updated Northeast");
        regionService.update(existingRegion);

        Region updatedRegion = regionService.get("2");
        Assert.assertEquals("Updated Northeast", updatedRegion.getRegion());
    }

    @Test
    public void getAllRegions_shouldReturnAllRegions() throws Exception {
        List<Region> regions = regionService.getAll();
        Assert.assertEquals(5, regions.size());
    }

    @Test
    public void deleteRegion_shouldRemoveAnExistingRegion() throws Exception {
        Region region = regionService.get("1");
        Assert.assertNotNull(region);

        Assert.assertEquals(5, regionService.getAll().size());

        regionService.delete(region);
        Assert.assertEquals(4, regionService.getAll().size());
    }

    private Region createRegion(String regionName, String regionId) {
        Region region = new Region();
        region.setRegion(regionName);
        region.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return region;
    }
}