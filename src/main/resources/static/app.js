document.addEventListener('DOMContentLoaded', () => {
    const presetSelect = document.getElementById('presetSelect');
    const applyPresetBtn = document.getElementById('applyPreset');
    const submitBtn = document.getElementById('submit');
    const closeModalBtn = document.getElementById('closeModalBtn'); // اضافه کردیم
    const resultModal = document.getElementById('resultModal');
    const simulationResultContent = document.getElementById('simulationResultContent');

    const datacenterList = document.getElementById('datacenterList');
    const hostList = document.getElementById('hostList');
    const vmList = document.getElementById('vmList');

    const addDatacenterBtn = document.getElementById('addDatacenter');
    const addHostBtn = document.getElementById('addHost');
    const addVmBtn = document.getElementById('addVm');

    let presets = {};

    // ---------------- Helpers ----------------
    function createInput(name, placeholder, type = "text", value = "") {
        const input = document.createElement("input");
        input.name = name;
        input.placeholder = placeholder;
        input.type = type;
        input.value = value;
        input.title=name;
        return input;
    }

    function removeButton(container) {
        const btn = document.createElement("button");
        btn.textContent = "Remove";
        btn.type = "button";
        btn.onclick = () => container.remove();
        return btn;
    }

    function createCard(fields, data = {}) {
        const card = document.createElement("div");
        card.className = "card";
        fields.forEach(f => {
            card.appendChild(createInput(f.name, f.placeholder, f.type || "text", data[f.name] || ""));
        });
        card.appendChild(removeButton(card));
        return card;
    }

    // ---------------- Add Resource Functions ----------------
    function addDatacenter(data = {}) {
        const fields = [
            {name: "architecture", placeholder: "Architecture"},
            {name: "os", placeholder: "OS"},
            {name: "vmm", placeholder: "VMM"},
            {name: "timeZone", placeholder: "Time Zone", type: "number"},
            {name: "costPerSec", placeholder: "Cost Per Sec", type: "number"},
            {name: "costPerMem", placeholder: "Cost Per Mem", type: "number"},
            {name: "costPerStorage", placeholder: "Cost Per Storage", type: "number"},
            {name: "costPerBw", placeholder: "Cost Per BW", type: "number"}
        ];
        const card = createCard(fields, data);
        datacenterList.appendChild(card);
    }

    function addHost(data = {}) {
        const fields = [
            {name: "ram", placeholder: "RAM", type: "number"},
            {name: "storage", placeholder: "Storage", type: "number"},
            {name: "bw", placeholder: "Bandwidth", type: "number"},
            {name: "pes", placeholder: "PEs", type: "number"},
            {name: "mipsPerPe", placeholder: "MIPS per PE", type: "number"}
        ];
        const card = createCard(fields, data);
        hostList.appendChild(card);
    }

    function addVm(data = {}) {
        const fields = [
            {name: "mips", placeholder: "MIPS", type: "number"},
            {name: "pes", placeholder: "PEs", type: "number"},
            {name: "ram", placeholder: "RAM", type: "number"},
            {name: "bw", placeholder: "Bandwidth", type: "number"},
            {name: "size", placeholder: "Size", type: "number"},
            {name: "vmm", placeholder: "VMM"}
        ];
        const card = createCard(fields, data);
        vmList.appendChild(card);
    }

    // ---------------- Fetch Presets from Backend ----------------
    fetch('/api/mobility-sim/presets')
        .then(res => res.json())
        .then(data => {
            presets = data; // expects { "LINUX": {...}, "WINDOWS": {...} }
            Object.keys(presets).forEach(key => {
                const option = document.createElement("option");
                option.value = key;
                option.textContent = presets[key].type;
                presetSelect.appendChild(option);
            });
        })
        .catch(err => console.error("Failed to load presets:", err));

    // ---------------- Apply Preset ----------------
    applyPresetBtn.addEventListener('click', () => {
        const type = presetSelect.value;
        if (!type || !presets[type]) return;

        const preset = presets[type];

        // Clear existing lists
        datacenterList.innerHTML = "";
        hostList.innerHTML = "";
        vmList.innerHTML = "";

        // Add all datacenters, hosts, and vms
        preset.datacenters.forEach(datacenter => addDatacenter(datacenter));
        preset.hosts.forEach(host => addHost(host));
        preset.vms.forEach(vm => addVm(vm));
    });

    // ---------------- Add Buttons ----------------
    addDatacenterBtn.addEventListener('click', () => addDatacenter());
    addHostBtn.addEventListener('click', () => addHost());
    addVmBtn.addEventListener('click', () => addVm());

    // ---------------- Submit ----------------

    // Fixing the Submit button issue
    submitBtn.addEventListener('click', () => {
        function collect(list) {
            return Array.from(list.children).map(card => {
                const obj = {};
                card.querySelectorAll('input').forEach(input => {
                    obj[input.name] = input.type === "number" ? Number(input.value) : input.value;
                });
                return obj;
            });
        }

        const payload = {
            datacenters: collect(datacenterList),
            hosts: collect(hostList),
            vms: collect(vmList)
        };

        fetch('/api/mobility-sim', {
            method: 'POST',
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(response => {
                // پر کردن نتایج در Modal
                let resultHTML = '<h3>Cloudlet Results</h3>';
                resultHTML += '<table>';
                resultHTML += '<thead><tr><th>Cloudlet ID</th><th>Status</th><th>VM ID</th><th>Start Time</th><th>Finish Time</th></tr></thead>';
                resultHTML += '<tbody>';

                response.cloudletStatuses.forEach(cloudlet => {
                    resultHTML += `
                    <tr>
                        <td>${cloudlet.cloudletId}</td>
                        <td>${cloudlet.status}</td>
                        <td>${cloudlet.vmId}</td>
                        <td>${cloudlet.startTime}</td>
                        <td>${cloudlet.finishTime}</td>
                    </tr>
                `;
                });

                resultHTML += '</tbody></table>';

                // نمایش اطلاعات در Modal
                simulationResultContent.innerHTML = resultHTML;
                resultModal.style.display = 'block'; // نمایش Modal
            })
            .catch(err => {
                alert("Failed to submit data!");
                console.error(err);
            });
    });

    // ---------------- Close Modal ----------------
    closeModalBtn.addEventListener('click', () => {
        resultModal.style.display = 'none'; // مخفی کردن Modal
    });

});